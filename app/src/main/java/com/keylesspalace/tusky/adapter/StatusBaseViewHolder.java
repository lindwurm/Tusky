package com.keylesspalace.tusky.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.entity.Account;
import com.keylesspalace.tusky.entity.Attachment;
import com.keylesspalace.tusky.entity.Attachment.Focus;
import com.keylesspalace.tusky.entity.Attachment.MetaData;
import com.keylesspalace.tusky.entity.Emoji;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.interfaces.LinkListener;
import com.keylesspalace.tusky.interfaces.StatusActionListener;
import com.keylesspalace.tusky.util.CustomEmojiHelper;
import com.keylesspalace.tusky.util.DateUtils;
import com.keylesspalace.tusky.util.HtmlUtils;
import com.keylesspalace.tusky.util.LinkHelper;
import com.keylesspalace.tusky.util.ThemeUtils;
import com.keylesspalace.tusky.view.MediaPreviewImageView;
import com.keylesspalace.tusky.viewdata.StatusViewData;
import com.mikepenz.iconics.utils.Utils;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import at.connyduck.sparkbutton.SparkButton;
import at.connyduck.sparkbutton.SparkEventListener;
import jp.kyori.tusky.RichTextUtil;

abstract class StatusBaseViewHolder extends RecyclerView.ViewHolder {

    private TextView displayName;
    private TextView username;
    private ImageButton replyButton;
    private SparkButton reblogButton;
    private SparkButton favouriteButton;
    private ImageButton quoteButton;
    private ImageButton moreButton;
    private boolean favourited;
    private boolean reblogged;
    private MediaPreviewImageView[] mediaPreviews;
    private ImageView[] mediaOverlays;
    private TextView sensitiveMediaWarning;
    private View sensitiveMediaShow;
    private TextView mediaLabel;
    private ToggleButton contentWarningButton;
    private RelativeLayout quoteContainer;

    ImageView avatar;
    TextView timestampInfo;
    TextView content;
    TextView contentWarningDescription;

    private boolean useAbsoluteTime;
    private SimpleDateFormat shortSdf;
    private SimpleDateFormat longSdf;

    StatusBaseViewHolder(View itemView, boolean useAbsoluteTime) {
        super(itemView);
        displayName = itemView.findViewById(R.id.status_display_name);
        username = itemView.findViewById(R.id.status_username);
        timestampInfo = itemView.findViewById(R.id.status_timestamp_info);
        content = itemView.findViewById(R.id.status_content);
        avatar = itemView.findViewById(R.id.status_avatar);

        replyButton = itemView.findViewById(R.id.status_reply);
        reblogButton = itemView.findViewById(R.id.status_reblog);
        favouriteButton = itemView.findViewById(R.id.status_favourite);
        quoteButton = itemView.findViewById(R.id.status_quote);
        moreButton = itemView.findViewById(R.id.status_more);
        reblogged = false;
        favourited = false;
        mediaPreviews = new MediaPreviewImageView[]{
                itemView.findViewById(R.id.status_media_preview_0),
                itemView.findViewById(R.id.status_media_preview_1),
                itemView.findViewById(R.id.status_media_preview_2),
                itemView.findViewById(R.id.status_media_preview_3)
        };
        mediaOverlays = new ImageView[]{
                itemView.findViewById(R.id.status_media_overlay_0),
                itemView.findViewById(R.id.status_media_overlay_1),
                itemView.findViewById(R.id.status_media_overlay_2),
                itemView.findViewById(R.id.status_media_overlay_3)
        };
        sensitiveMediaWarning = itemView.findViewById(R.id.status_sensitive_media_warning);
        sensitiveMediaShow = itemView.findViewById(R.id.status_sensitive_media_button);
        mediaLabel = itemView.findViewById(R.id.status_media_label);
        contentWarningDescription = itemView.findViewById(R.id.status_content_warning_description);
        contentWarningButton = itemView.findViewById(R.id.status_content_warning_button);

        quoteContainer = itemView.findViewById(R.id.status_quote_inline_container);

        this.useAbsoluteTime = useAbsoluteTime;
        shortSdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        longSdf = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());
    }

    protected abstract int getMediaPreviewHeight(Context context);

    private void setDisplayName(String name, List<Emoji> customEmojis) {
        CharSequence emojifiedName = CustomEmojiHelper.emojifyString(name, customEmojis, displayName);
        displayName.setText(emojifiedName);
    }

    private void setUsername(String name) {
        Context context = username.getContext();
        String format = context.getString(R.string.status_username_format);
        String usernameText = String.format(format, name);
        username.setText(usernameText);
    }

    private void setSpoilerAndContent(StatusViewData.Concrete status,
                                      final StatusActionListener listener, boolean removeQuote) {
        if (status.getSpoilerText() == null || status.getSpoilerText().isEmpty()) {
            contentWarningDescription.setVisibility(View.GONE);
            contentWarningButton.setVisibility(View.GONE);
            this.setTextVisible(true, status, listener, removeQuote);
        } else {
            boolean expanded = status.isExpanded();
            CharSequence emojiSpoiler = CustomEmojiHelper.emojifyString(
                    status.getSpoilerText(), status.getStatusEmojis(), contentWarningDescription);
            contentWarningDescription.setText(emojiSpoiler);
            contentWarningDescription.setVisibility(View.VISIBLE);
            contentWarningButton.setVisibility(View.VISIBLE);
            contentWarningButton.setChecked(expanded);
            contentWarningButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                contentWarningDescription.invalidate();
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onExpandedChange(isChecked, getAdapterPosition());
                }
                this.setTextVisible(isChecked, status, listener, removeQuote);
            });
            this.setTextVisible(expanded, status, listener, removeQuote);
        }
    }

    private void setTextVisible(boolean expanded, StatusViewData.Concrete status,
                                final StatusActionListener listener, boolean removeQuote) {
        Status.Mention[] mentions = status.getMentions();
        if (expanded) {
            Spanned emojifiedText = CustomEmojiHelper.emojifyText(
                    status.getContent(), status.getStatusEmojis(), this.content);
            LinkHelper.setClickableText(this.content, emojifiedText, mentions, listener, removeQuote);
        } else {
            LinkHelper.setClickableMentions(this.content, mentions, listener);
        }
        if (TextUtils.isEmpty(this.content.getText())) {
            this.content.setVisibility(View.GONE);
        } else {
            this.content.setVisibility(View.VISIBLE);
        }
    }

    void setAvatar(String url, @Nullable String rebloggedUrl) {
        if (TextUtils.isEmpty(url)) {
            avatar.setImageResource(R.drawable.avatar_default);
        } else {
            Picasso.with(avatar.getContext())
                    .load(url)
                    .placeholder(R.drawable.avatar_default)
                    .into(avatar);
        }
    }

    protected void setCreatedAt(@Nullable Date createdAt) {
        if (useAbsoluteTime) {
            String time;
            if (createdAt != null) {
                if (System.currentTimeMillis() - createdAt.getTime() > 86400000L) {
                    time = longSdf.format(createdAt);
                } else {
                    time = shortSdf.format(createdAt);
                }
            } else {
                time = "??:??:??";
            }
            timestampInfo.setText(time);
        } else {
            // This is the visible timestampInfo.
            String readout;
            /* This one is for screen-readers. Frequently, they would mispronounce timestamps like "17m"
             * as 17 meters instead of minutes. */
            CharSequence readoutAloud;
            if (createdAt != null) {
                long then = createdAt.getTime();
                long now = new Date().getTime();
                readout = DateUtils.getRelativeTimeSpanString(timestampInfo.getContext(), then, now);
                readoutAloud = android.text.format.DateUtils.getRelativeTimeSpanString(then, now,
                        android.text.format.DateUtils.SECOND_IN_MILLIS,
                        android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE);
            } else {
                // unknown minutes~
                readout = "?m";
                readoutAloud = "? minutes";
            }
            timestampInfo.setText(readout);
            timestampInfo.setContentDescription(readoutAloud);
        }
    }

    private void setStatusVisibility(Status.Visibility visibility) {
        if (visibility == null || this.timestampInfo == null) {
            return;
        }

        int visibilityIcon;
        switch (visibility) {
            case PUBLIC:
                visibilityIcon = R.drawable.ic_public_24dp;
                break;
            case UNLISTED:
                visibilityIcon = R.drawable.ic_lock_open_24dp;
                break;
            case PRIVATE:
                visibilityIcon = R.drawable.ic_lock_outline_24dp;
                break;
            case UNLEAKABLE:
                visibilityIcon = R.drawable.ic_unleakable_24dp;
                break;
            case DIRECT:
                visibilityIcon = R.drawable.ic_email_24dp;
                break;
            default:
                return;
        }

        final Drawable visibilityDrawable = this.timestampInfo.getContext()
                .getDrawable(visibilityIcon);
        if (visibilityDrawable == null) {
            return;
        }

        final int size = (int) this.timestampInfo.getTextSize();
        visibilityDrawable.setBounds(
                0,
                0,
                size,
                size
        );
        visibilityDrawable.setTint(this.timestampInfo.getCurrentTextColor());
        this.timestampInfo.setCompoundDrawables(
                visibilityDrawable,
                null,
                null,
                null
        );
    }

    protected void showContent(boolean show) {
        if (show) {
            itemView.setVisibility(View.VISIBLE);
            itemView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            itemView.setVisibility(View.INVISIBLE);
            itemView.getLayoutParams().height = Utils.convertDpToPx(itemView.getContext(), 24);
        }
    }

    private void setIsReply(boolean isReply) {
        if (isReply) {
            replyButton.setImageResource(R.drawable.ic_reply_all_24dp);
        } else {
            replyButton.setImageResource(R.drawable.ic_reply_24dp);
        }

    }

    private void setReblogged(boolean reblogged) {
        this.reblogged = reblogged;
        reblogButton.setChecked(reblogged);
    }

    // This should only be called after setReblogged, in order to override the tint correctly.
    private void setRebloggingEnabled(boolean enabled, Status.Visibility visibility) {
        reblogButton.setEnabled(enabled && visibility != Status.Visibility.PRIVATE && visibility != Status.Visibility.UNLEAKABLE);

        if (enabled) {
            int inactiveId;
            int activeId;
            if (visibility == Status.Visibility.PRIVATE) {
                inactiveId = ThemeUtils.getDrawableId(reblogButton.getContext(),
                        R.attr.status_reblog_disabled_private_drawable, R.drawable.reblog_private_dark);
                activeId = R.drawable.reblog_private_active;
            } else if (visibility == Status.Visibility.UNLEAKABLE) {
                inactiveId = ThemeUtils.getDrawableId(reblogButton.getContext(),
                        R.attr.status_reblog_disabled_unleakable_drawable, R.drawable.reblog_unleakable_dark);
                activeId = R.drawable.reblog_unleakable_active;
            } else {
                inactiveId = ThemeUtils.getDrawableId(reblogButton.getContext(),
                        R.attr.status_reblog_inactive_drawable, R.drawable.reblog_inactive_dark);
                activeId = R.drawable.reblog_active;
            }
            reblogButton.setInactiveImage(inactiveId);
            reblogButton.setActiveImage(activeId);
        } else {
            int disabledId;
            if (visibility == Status.Visibility.DIRECT) {
                disabledId = ThemeUtils.getDrawableId(reblogButton.getContext(),
                        R.attr.status_reblog_direct_drawable, R.drawable.reblog_direct_dark);
            } else {
                disabledId = ThemeUtils.getDrawableId(reblogButton.getContext(),
                        R.attr.status_reblog_disabled_private_drawable, R.drawable.reblog_private_dark);
            }
            reblogButton.setInactiveImage(disabledId);
            reblogButton.setActiveImage(disabledId);
        }
    }

    private void setQuoteEnabled(boolean enabled, Status.Visibility visibility) {
        quoteButton.setEnabled(enabled && visibility != Status.Visibility.PRIVATE && visibility != Status.Visibility.UNLEAKABLE);

        if (enabled && visibility != Status.Visibility.PRIVATE && visibility != Status.Visibility.UNLEAKABLE) {
            int activeId;
            activeId = ThemeUtils.getDrawableId(quoteButton.getContext(),
                    R.attr.status_quote_drawable, R.drawable.ic_quote_24dp);
            quoteButton.setImageResource(activeId);
        } else {
            //int disabledId;
            //disabledId = ThemeUtils.getDrawableId(quoteButton.getContext(),
            //        R.attr.status_quote_disabled_drawable, R.drawable.ic_quote_disabled_24dp);
            //quoteButton.setImageResource(disabledId);

            Resources res = quoteButton.getContext().getResources();
            Drawable disableIcon = res.getDrawable(R.drawable.ic_quote_disabled_24dp);
            if (disableIcon != null) {
                disableIcon.setColorFilter(res.getColor(R.color.status_reblog_button_disabled_dark), PorterDuff.Mode.DST_IN);
            }
            quoteButton.setImageDrawable(disableIcon);
        }
    }

    private void setFavourited(boolean favourited) {
        this.favourited = favourited;
        favouriteButton.setChecked(favourited);
    }

    private void setQuoteContainer(Status status, final StatusActionListener listener) {
        if (status != null) {
            quoteContainer.setVisibility(View.VISIBLE);
            new QuoteInlineHelper(status, quoteContainer, listener).setupQuoteContainer();
        } else {
            quoteContainer.setVisibility(View.GONE);
        }
    }

    private void setMediaPreviews(final List<Attachment> attachments, boolean sensitive,
                                  final StatusActionListener listener, boolean showingContent) {

        Context context = itemView.getContext();

        int mediaPreviewUnloadedId =
                ThemeUtils.getDrawableId(itemView.getContext(), R.attr.media_preview_unloaded_drawable,
                        android.R.color.black);

        final int n = Math.min(attachments.size(), Status.MAX_MEDIA_ATTACHMENTS);

        for (int i = 0; i < n; i++) {
            String previewUrl = attachments.get(i).getPreviewUrl();
            String description = attachments.get(i).getDescription();

            if (TextUtils.isEmpty(description)) {
                mediaPreviews[i].setContentDescription(context.getString(R.string.action_view_media));
            } else {
                mediaPreviews[i].setContentDescription(description);
            }

            mediaPreviews[i].setVisibility(View.VISIBLE);

            if (TextUtils.isEmpty(previewUrl)) {
                Picasso.with(context)
                        .load(mediaPreviewUnloadedId)
                        .into(mediaPreviews[i]);
            } else {
                MetaData meta = attachments.get(i).getMeta();
                Focus focus = meta != null ? meta.getFocus() : null;

                if (focus != null) { // If there is a focal point for this attachment:
                    mediaPreviews[i].setFocalPoint(focus);

                    Picasso.with(context)
                            .load(previewUrl)
                            .placeholder(mediaPreviewUnloadedId)
                            // Also pass the mediaPreview as a callback to ensure it is called
                            // initially when the image gets loaded:
                            .into(mediaPreviews[i], mediaPreviews[i]);
                } else {
                    mediaPreviews[i].removeFocalPoint();

                    Picasso.with(context)
                            .load(previewUrl)
                            .placeholder(mediaPreviewUnloadedId)
                            .into(mediaPreviews[i]);
                }
            }

            final Attachment.Type type = attachments.get(i).getType();
            if (type == Attachment.Type.VIDEO | type == Attachment.Type.GIFV) {
                mediaOverlays[i].setVisibility(View.VISIBLE);
            } else {
                mediaOverlays[i].setVisibility(View.GONE);
            }

            final int urlIndex = i;
            mediaPreviews[i].setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onViewMedia(getAdapterPosition(), urlIndex, v);
                }
            });

            if (n <= 2) {
                mediaPreviews[0].getLayoutParams().height = getMediaPreviewHeight(context) * 2;
                mediaPreviews[1].getLayoutParams().height = getMediaPreviewHeight(context) * 2;
            } else {
                mediaPreviews[0].getLayoutParams().height = getMediaPreviewHeight(context);
                mediaPreviews[1].getLayoutParams().height = getMediaPreviewHeight(context);
                mediaPreviews[2].getLayoutParams().height = getMediaPreviewHeight(context);
                mediaPreviews[3].getLayoutParams().height = getMediaPreviewHeight(context);
            }
        }

        String hiddenContentText;
        if (sensitive) {
            hiddenContentText = context.getString(R.string.status_sensitive_media_template,
                    context.getString(R.string.status_sensitive_media_title),
                    context.getString(R.string.status_sensitive_media_directions));
        } else {
            hiddenContentText = context.getString(R.string.status_sensitive_media_template,
                    context.getString(R.string.status_media_hidden_title),
                    context.getString(R.string.status_sensitive_media_directions));
        }

        sensitiveMediaWarning.setText(HtmlUtils.fromHtml(hiddenContentText));

        sensitiveMediaWarning.setVisibility(showingContent ? View.GONE : View.VISIBLE);
        sensitiveMediaShow.setVisibility(showingContent ? View.VISIBLE : View.GONE);
        sensitiveMediaShow.setOnClickListener(v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                listener.onContentHiddenChange(false, getAdapterPosition());
            }
            v.setVisibility(View.GONE);
            sensitiveMediaWarning.setVisibility(View.VISIBLE);
        });
        sensitiveMediaWarning.setOnClickListener(v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                listener.onContentHiddenChange(true, getAdapterPosition());
            }
            v.setVisibility(View.GONE);
            sensitiveMediaShow.setVisibility(View.VISIBLE);
        });


        // Hide any of the placeholder previews beyond the ones set.
        for (int i = n; i < Status.MAX_MEDIA_ATTACHMENTS; i++) {
            mediaPreviews[i].setVisibility(View.GONE);
        }
    }

    @NonNull
    private static String getLabelTypeText(Context context, Attachment.Type type) {
        switch (type) {
            default:
            case IMAGE:
                return context.getString(R.string.status_media_images);
            case GIFV:
            case VIDEO:
                return context.getString(R.string.status_media_video);
        }
    }

    @DrawableRes
    private static int getLabelIcon(Attachment.Type type) {
        switch (type) {
            default:
            case IMAGE:
                return R.drawable.ic_photo_24dp;
            case GIFV:
            case VIDEO:
                return R.drawable.ic_videocam_24dp;
        }
    }

    private void setMediaLabel(List<Attachment> attachments, boolean sensitive,
                               final StatusActionListener listener) {
        if (attachments.size() == 0) {
            mediaLabel.setVisibility(View.GONE);
            return;
        }
        mediaLabel.setVisibility(View.VISIBLE);

        // Set the label's text.
        Context context = itemView.getContext();
        String labelText = getLabelTypeText(context, attachments.get(0).getType());
        if (sensitive) {
            String sensitiveText = context.getString(R.string.status_sensitive_media_title);
            labelText += String.format(" (%s)", sensitiveText);
        }
        mediaLabel.setText(labelText);

        // Set the icon next to the label.
        int drawableId = getLabelIcon(attachments.get(0).getType());
        Drawable drawable = AppCompatResources.getDrawable(context, drawableId);
        ThemeUtils.setDrawableTint(context, drawable, android.R.attr.textColorTertiary);
        mediaLabel.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);

        mediaLabel.setOnClickListener(v -> listener.onViewMedia(getAdapterPosition(), 0, null));
    }

    private void hideSensitiveMediaWarning() {
        sensitiveMediaWarning.setVisibility(View.GONE);
        sensitiveMediaShow.setVisibility(View.GONE);
    }

    private void setupButtons(final StatusActionListener listener, final String accountId) {
        /* Originally position was passed through to all these listeners, but it caused several
         * bugs where other statuses in the list would be removed or added and cause the position
         * here to become outdated. So, getting the adapter position at the time the listener is
         * actually called is the appropriate solution. */
        avatar.setOnClickListener(v -> listener.onViewAccount(accountId));
        replyButton.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onReply(position);
            }
        });
        replyButton.setOnLongClickListener(v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onFavourite(!favourited, position);
                listener.onReblog(!reblogged, position);
            }
            return true;
        });
        replyButton.setLongClickable(true);
        reblogButton.setEventListener(new SparkEventListener() {
            @Override
            public void onEvent(ImageView button, boolean buttonState) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onReblog(!reblogged, position);
                }
            }

            @Override
            public void onEventAnimationEnd(ImageView button, boolean buttonState) {
            }

            @Override
            public void onEventAnimationStart(ImageView button, boolean buttonState) {
            }
        });
        favouriteButton.setEventListener(new SparkEventListener() {
            @Override
            public void onEvent(ImageView button, boolean buttonState) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onFavourite(!favourited, position);
                }
            }

            @Override
            public void onEventAnimationEnd(ImageView button, boolean buttonState) {
            }

            @Override
            public void onEventAnimationStart(ImageView button, boolean buttonState) {
            }
        });
        quoteButton.setOnClickListener(view -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onQuote(position);
            }
        });
        moreButton.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onMore(v, position);
            }
        });
        /* Even though the content TextView is a child of the container, it won't respond to clicks
         * if it contains URLSpans without also setting its listener. The surrounding spans will
         * just eat the clicks instead of deferring to the parent listener, but WILL respond to a
         * listener directly on the TextView, for whatever reason. */
        View.OnClickListener viewThreadListener = v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onViewThread(position);
            }
        };
        content.setOnClickListener(viewThreadListener);
        itemView.setOnClickListener(viewThreadListener);
    }

    void setupWithStatus(StatusViewData.Concrete status, final StatusActionListener listener,
                         boolean mediaPreviewEnabled) {
        setDisplayName(status.getUserFullName(), status.getAccountEmojis());
        setUsername(status.getNickname());
        setCreatedAt(status.getCreatedAt());
        setStatusVisibility(status.getVisibility());
        setIsReply(status.getInReplyToId() != null);
        setAvatar(status.getAvatar(), status.getRebloggedAvatar());
        setReblogged(status.isReblogged());
        setFavourited(status.isFavourited());
        setQuoteContainer(status.getQuote(), listener);
        List<Attachment> attachments = status.getAttachments();
        boolean sensitive = status.isSensitive();
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        if (mediaPreviewEnabled) {
            setMediaPreviews(attachments, sensitive, listener, status.isShowingContent());

            if (attachments.size() == 0) {
                hideSensitiveMediaWarning();
            }
            // Hide the unused label.
            mediaLabel.setVisibility(View.GONE);
        } else {
            setMediaLabel(attachments, sensitive, listener);
            // Hide all unused views.
            mediaPreviews[0].setVisibility(View.GONE);
            mediaPreviews[1].setVisibility(View.GONE);
            mediaPreviews[2].setVisibility(View.GONE);
            mediaPreviews[3].setVisibility(View.GONE);
            hideSensitiveMediaWarning();
        }

        setupButtons(listener, status.getSenderId());
        setRebloggingEnabled(status.getRebloggingEnabled(), status.getVisibility());
        setQuoteEnabled(status.getRebloggingEnabled(), status.getVisibility());

        setSpoilerAndContent(status, listener, status.getQuote() != null);

    }

    static class QuoteInlineHelper {
        Status quoteStatus;

        private View quoteContainer;
        private ImageView quoteAvatar;
        private TextView quoteDisplayName;
        private TextView quoteUsername;
        private TextView quoteContentWarningDescription;
        private ToggleButton quoteContentWarningButton;
        private TextView quoteContent;
        private TextView quoteMedia;

        private LinkListener listener;

        QuoteInlineHelper(Status status, View container, LinkListener listener) {
            quoteStatus = status;
            quoteContainer = container;
            quoteAvatar = container.findViewById(R.id.status_quote_inline_avatar);
            quoteDisplayName = container.findViewById(R.id.status_quote_inline_display_name);
            quoteUsername = container.findViewById(R.id.status_quote_inline_username);
            quoteContentWarningDescription = container.findViewById(R.id.status_quote_inline_content_warning_description);
            quoteContentWarningButton = container.findViewById(R.id.status_quote_inline_content_warning_button);
            quoteContent = container.findViewById(R.id.status_quote_inline_content);
            quoteMedia = container.findViewById(R.id.status_quote_inline_media);
            this.listener = listener;
        }

        private void setDisplayName(String name, List<Emoji> customEmojis) {
            CharSequence emojifiedName = CustomEmojiHelper.emojifyString(name, customEmojis, quoteDisplayName);
            quoteDisplayName.setText(emojifiedName);
        }

        private void setUsername(String name) {
            Context context = quoteUsername.getContext();
            String format = context.getString(R.string.status_username_format);
            String usernameText = String.format(format, name);
            quoteUsername.setText(usernameText);
        }

        private void setContent(Spanned content, Status.Mention[] mentions, List<Emoji> emojis,
                                LinkListener listener) {
            Spanned singleLineText = RichTextUtil.replaceSpanned(content, "\n", " ");
            Spanned emojifiedText = CustomEmojiHelper.emojifyText(singleLineText, emojis, quoteContent);

            LinkHelper.setClickableText(quoteContent, emojifiedText, mentions, listener, false);
        }

        private void setAvatar(String url) {
            if (TextUtils.isEmpty(url)) {
                quoteAvatar.setImageResource(R.drawable.avatar_default);
            } else {
                Picasso.with(quoteAvatar.getContext())
                        .load(url)
                        .placeholder(R.drawable.avatar_default)
                        .into(quoteAvatar);
            }
        }

        private void setSpoilerText(String spoilerText, List<Emoji> emojis, final boolean expanded) {
            CharSequence emojiSpoiler =
                    CustomEmojiHelper.emojifyString(spoilerText, emojis, quoteContentWarningDescription);
            quoteContentWarningDescription.setText(emojiSpoiler);
            quoteContentWarningDescription.setVisibility(View.VISIBLE);
            quoteContentWarningButton.setVisibility(View.VISIBLE);
            quoteContentWarningButton.setChecked(expanded);
            quoteContentWarningButton.setOnCheckedChangeListener((buttonView, isChecked)
                    -> quoteContent.setVisibility(isChecked ? View.VISIBLE : View.GONE));
            quoteContent.setVisibility(expanded ? View.VISIBLE : View.GONE);

        }

        private void hideSpoilerText() {
            quoteContentWarningDescription.setVisibility(View.GONE);
            quoteContentWarningButton.setVisibility(View.GONE);
            quoteContent.setVisibility(View.VISIBLE);
        }

        private void setOnClickListener(String accountId, String statusUrl) {
            quoteAvatar.setOnClickListener(view -> listener.onViewAccount(accountId));
            quoteDisplayName.setOnClickListener(view -> listener.onViewAccount(accountId));
            quoteUsername.setOnClickListener(view -> listener.onViewAccount(accountId));
            quoteContent.setOnClickListener(view -> listener.onViewUrl(statusUrl, "quote_inline"));
            quoteMedia.setOnClickListener(view -> listener.onViewUrl(statusUrl, "quote_inline"));
            quoteContainer.setOnClickListener(view -> listener.onViewUrl(statusUrl, "quote_inline"));
        }

        void setupQuoteContainer() {
            Account account = quoteStatus.getAccount();
            setDisplayName(account.getDisplayName().equals("") ? account.getLocalUsername() : account.getDisplayName(), account.getEmojis());
            setUsername(account.getUsername());
            setContent(quoteStatus.getContent(), quoteStatus.getMentions(),
                    quoteStatus.getEmojis(), listener);
            setAvatar(account.getAvatar());
            setOnClickListener(account.getId(), quoteStatus.getUrl());

            if (quoteStatus.getSpoilerText().isEmpty()) {
                hideSpoilerText();
            } else {
                setSpoilerText(quoteStatus.getSpoilerText(), quoteStatus.getEmojis(), false);
            }

            if (quoteStatus.getAttachments().size() == 0) {
                quoteMedia.setVisibility(View.GONE);
            } else {
                quoteMedia.setVisibility(View.VISIBLE);
                quoteMedia.setText(quoteContainer.getContext().getString(R.string.quote_status_media_notice,
                        quoteStatus.getAttachments().size()));
            }
        }
    }
}
