/* Copyright 2019 Conny Duck
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.components.conversation

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.keylesspalace.tusky.AccountActivity
import com.keylesspalace.tusky.ComposeActivity
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.ViewTagActivity
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.di.Injectable
import com.keylesspalace.tusky.di.ViewModelFactory
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.fragment.SFragment
import com.keylesspalace.tusky.interfaces.StatusActionListener
import com.keylesspalace.tusky.network.TimelineCases
import com.keylesspalace.tusky.util.NetworkState
import com.keylesspalace.tusky.util.ThemeUtils
import com.keylesspalace.tusky.util.hide
import kotlinx.android.synthetic.main.fragment_timeline.*
import java.util.*
import javax.inject.Inject

class ConversationsFragment : SFragment(), StatusActionListener, Injectable {

    @Inject
    lateinit var timelineCases: TimelineCases
    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    @Inject
    lateinit var db: AppDatabase
    @Inject
    lateinit var eventHub: EventHub

    private lateinit var viewModel: ConversationsViewModel

    private lateinit var adapter: ConversationAdapter

    private var inReplyTo: Status? = null

    private lateinit var preferences: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProviders.of(this, viewModelFactory)[ConversationsViewModel::class.java]

        return inflater.inflate(R.layout.fragment_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        preferences = PreferenceManager.getDefaultSharedPreferences(view.context)
        val useAbsoluteTime = preferences.getBoolean("absoluteTimeView", false)

        val account = accountManager.activeAccount
        val mediaPreviewEnabled = account?.mediaPreviewEnabled ?: true


        adapter = ConversationAdapter(useAbsoluteTime, mediaPreviewEnabled, this, ::onTopLoaded, viewModel::retry)

        recyclerView.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = adapter
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        progressBar.hide()
        statusView.hide()

        initSwipeToRefresh()

        viewModel.conversations.observe(this, Observer<PagedList<ConversationEntity>> {
            adapter.submitList(it)
        })
        viewModel.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })

        viewModel.load()

        floating_btn.setOnClickListener {
            if (toot_edit_text.text.isEmpty() && inReplyTo == null) {
                val composeIntent = Intent(context, ComposeActivity::class.java)
                startActivity(composeIntent)
            } else {
                startComposeWithQuickComposeData()
            }
        }

        updateQuickComposeInfo()
        updateVisibilityButton()
        visibility_button.setOnClickListener { setNextVisibility() }
        toot_button.setOnClickListener { quickToot(it) }
        recyclerView.requestFocus()

    }

    private fun initSwipeToRefresh() {
        viewModel.refreshState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        })
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.tusky_blue)
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(ThemeUtils.getColor(swipeRefreshLayout.context, android.R.attr.colorBackground))
    }

    private fun onTopLoaded() {
        recyclerView.scrollToPosition(0)
    }

    override fun onReblog(reblog: Boolean, position: Int) {
        // its impossible to reblog private messages
    }

    override fun onFavourite(favourite: Boolean, position: Int) {
        viewModel.favourite(favourite, position)
    }

    override fun onQuote(position: Int) {
        // its impossible to quote private messages
    }

    override fun onMore(view: View, position: Int) {
        viewModel.conversations.value?.getOrNull(position)?.lastStatus?.let {
            more(it.toStatus(), view, position)
        }
    }

    override fun onViewMedia(position: Int, attachmentIndex: Int, view: View?) {
        viewModel.conversations.value?.getOrNull(position)?.lastStatus?.let {
            viewMedia(attachmentIndex, it.toStatus(), view)
        }
    }

    override fun onViewThread(position: Int) {
        viewModel.conversations.value?.getOrNull(position)?.lastStatus?.let {
            viewThread(it.toStatus())
        }
    }

    override fun onOpenReblog(position: Int) {
        // there are no reblogs in search results
    }

    override fun onExpandedChange(expanded: Boolean, position: Int) {
        viewModel.expandHiddenStatus(expanded, position)
    }

    override fun onContentHiddenChange(isShowing: Boolean, position: Int) {
        viewModel.showContent(isShowing, position)
    }

    override fun onLoadMore(position: Int) {
        // not using the old way of pagination
    }

    override fun onContentCollapsedChange(isCollapsed: Boolean, position: Int) {
        viewModel.collapseLongStatus(isCollapsed, position)
    }

    override fun onViewAccount(id: String) {
        val intent = AccountActivity.getIntent(requireContext(), id)
        startActivity(intent)
    }

    override fun onViewTag(tag: String) {
        val intent = Intent(context, ViewTagActivity::class.java)
        intent.putExtra("hashtag", tag)
        startActivity(intent)
    }

    override fun timelineCases(): TimelineCases {
        return timelineCases
    }

    override fun removeItem(position: Int) {
        viewModel.remove(position)
    }

    override fun onReply(position: Int) {
        viewModel.conversations.value?.getOrNull(position)?.lastStatus?.let {
            reply(it.toStatus())
        }
    }

    private fun quickToot(v: View) {
        if (toot_edit_text.text.toString().isNotEmpty()) {
            val composeIntent = setupIntentBuilder()
                    .tootRightNow(true)
                    .build(v.context)

            resetQuickCompose()
            v.context.startActivity(composeIntent)
        }
    }

    private fun updateQuickComposeInfo() {
        updateQuickReplyInfo()
        updateDefaultTagInfo()
    }

    private fun updateQuickReplyInfo() {
        if (inReplyTo != null) {
            quick_reply_info.text = String.format("Reply to : %s", inReplyTo!!.account.username)
        } else {
            quick_reply_info.text = ""
        }
    }

    private fun updateDefaultTagInfo() {
        val useDefaultTag = preferences.getBoolean("use_default_text", false)
        val defaultText = preferences.getString("default_text", "")
        if (useDefaultTag) {
            default_tag_info.text = String.format("%s : %s", getString(R.string.hint_default_text), defaultText)
            if (preferences.getString("appTheme", ThemeUtils.APP_THEME_DEFAULT) == ThemeUtils.THEME_DAY) {
                default_tag_info.setTextColor(Color.RED)
            } else {
                default_tag_info.setTextColor(Color.YELLOW)
            }
        } else {
            default_tag_info.text = String.format("%s inactive", getString(R.string.hint_default_text))
            default_tag_info.setTextColor(Color.GRAY)
        }
    }

    private fun startComposeWithQuickComposeData() {
        val composeIntent = setupIntentBuilder()
                .build(context)
        resetQuickCompose()
        startActivity(composeIntent)
    }

    private fun setupIntentBuilder(): ComposeActivity.IntentBuilder {
        val builder = ComposeActivity.IntentBuilder()
        return addComposeData(builder)
    }

    private fun addComposeData(builder: ComposeActivity.IntentBuilder): ComposeActivity.IntentBuilder {
        var intentBuilder = builder
        var content = toot_edit_text.text.toString()
        if (preferences.getBoolean("use_default_text", false)) {
            content += " " + preferences.getString("default_text", "")!!
        }

        intentBuilder = intentBuilder.savedVisibility(getCurrentVisibility())

        if (inReplyTo != null) {
            val mentions = inReplyTo!!.mentions
            val mentionedUsernames = LinkedHashSet<String>()
            mentionedUsernames.add(inReplyTo!!.account.username)
            var loggedInUsername: String? = null
            val activeAccount = accountManager.activeAccount
            if (activeAccount != null) {
                loggedInUsername = activeAccount.username
            }
            for (mention in mentions) {
                mentionedUsernames.add(mention.username!!)
            }
            mentionedUsernames.remove(loggedInUsername)


            content = joinMentions(mentionedUsernames) + content

            intentBuilder = intentBuilder.inReplyToId(inReplyTo!!.id)
                    .savedVisibility(inReplyTo!!.visibility)
                    .contentWarning(inReplyTo!!.spoilerText)
                    .mentionedUsernames(mentionedUsernames)
                    .replyingStatusAuthor(inReplyTo!!.account.localUsername)
                    .replyingStatusContent(inReplyTo!!.content.toString())
        }

        return intentBuilder.savedTootText(content)
    }

    private fun joinMentions(mentionedUsernames: Set<String>): String {
        val builder = StringBuilder()
        for (name in mentionedUsernames) {
            builder.append('@')
            builder.append(name)
            builder.append(' ')
        }
        return builder.toString()
    }

    private fun resetQuickCompose() {
        toot_edit_text.text.clear()
        inReplyTo = null
        updateQuickReplyInfo()
    }

    private fun getCurrentVisibility(): Status.Visibility {
        val visibility = Status.Visibility.byNum(preferences.getInt("current_visibility", 1))
        if (!Arrays.asList(*ComposeActivity.CAN_USE_UNLEAKABLE)
                        .contains(accountManager.activeAccount!!.domain) && visibility === Status.Visibility.UNLEAKABLE) {
            preferences.edit()
                    .putInt("current_visibility", Status.Visibility.PUBLIC.num)
                    .apply()
            eventHub.dispatch(PreferenceChangedEvent("current_visibility"))
            return Status.Visibility.PUBLIC
        }
        return visibility
    }

    private fun updateVisibilityButton() {
        val visibility = getCurrentVisibility()
        when (visibility) {
            Status.Visibility.PUBLIC -> visibility_button.setImageResource(R.drawable.ic_public_24dp)
            Status.Visibility.UNLISTED -> visibility_button.setImageResource(R.drawable.ic_lock_open_24dp)
            Status.Visibility.PRIVATE -> visibility_button.setImageResource(R.drawable.ic_lock_outline_24dp)
            Status.Visibility.UNLEAKABLE -> visibility_button.setImageResource(R.drawable.ic_unleakable_24dp)
            else -> visibility_button.setImageResource(R.drawable.ic_public_24dp)
        }
    }

    private fun setNextVisibility() {
        val visibility = when (getCurrentVisibility()) {
            Status.Visibility.PUBLIC -> Status.Visibility.UNLISTED
            Status.Visibility.UNLISTED -> Status.Visibility.PRIVATE
            Status.Visibility.PRIVATE -> if (Arrays.asList(*ComposeActivity.CAN_USE_UNLEAKABLE)
                            .contains(accountManager.activeAccount!!.domain)) {
                Status.Visibility.UNLEAKABLE
            } else {
                Status.Visibility.PUBLIC
            }
            Status.Visibility.UNLEAKABLE -> Status.Visibility.PUBLIC
            Status.Visibility.UNKNOWN -> Status.Visibility.PUBLIC
            else -> Status.Visibility.PUBLIC
        }
        preferences.edit()
                .putInt("current_visibility", visibility.num)
                .apply()
        eventHub.dispatch(PreferenceChangedEvent("current_visibility"))
        updateVisibilityButton()
    }

    private fun onPreferenceChanged(key: String) {
        when (key) {
            "current_visibility" -> updateVisibilityButton()
            "use_default_text" -> updateQuickComposeInfo()
            "default_text" -> updateQuickComposeInfo()
        }
    }

    companion object {
        fun newInstance() = ConversationsFragment()
    }
}
