package com.fusionx.lightirc.irc;

import android.content.Context;
import android.text.Spanned;
import android.widget.Checkable;

import com.fusionx.lightirc.R;
import com.fusionx.lightirc.collections.UpdateableTreeSet;
import com.fusionx.lightirc.constants.UserLevelEnum;
import com.fusionx.lightirc.util.ColourParserUtils;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;

import lombok.NonNull;

public class ChannelUser extends User implements UpdateableTreeSet.Updateable, Checkable {
    private final HashMap<Channel, UserLevelEnum> userLevelMap = new HashMap<Channel, UserLevelEnum>();
    private final HashMap<Channel, Spanned> channelSpannableHashMap = new HashMap<Channel, Spanned>();
    private boolean mChecked = false;
    protected final Server mServer;

    public ChannelUser(@NonNull String nick, @NonNull UserChannelInterface userChannelInterface) {
        super(nick, userChannelInterface);
        mServer = userChannelInterface.getServer();
    }

    public String getPrettyNick(final String channel) {
        return getPrettyNick(userChannelInterface.getChannel(channel));
    }

    public String getPrettyNick(final Channel channel) {
        return String.format(nickHTML, getUserPrefix(channel) + nick);
    }

    public Spanned getSpannableNick(final Channel channel) {
        Spanned spannable = channelSpannableHashMap.get(channel);
        if (spannable == null) {
            updateSpannableNick(channel);
        }
        return spannable;
    }

    public String getBracketedNick(final Channel channel) {
        return String.format(nickHTML, "<" + getUserPrefix(channel) + nick + ">");
    }

    public void onJoin(final Channel channel) {
        userLevelMap.put(channel, UserLevelEnum.NONE);
        updateSpannableNick(channel);
    }

    public void onRemove(final Channel channel) {
        userLevelMap.remove(channel);
        channelSpannableHashMap.remove(channel);
    }

    public UserLevelEnum getChannelPrivileges(final Channel channel) {
        return userLevelMap.get(channel);
    }

    private void updateSpannableNick(final Channel channel) {
        Spanned spannable = ColourParserUtils.parseMarkup(getPrettyNick(channel));
        channelSpannableHashMap.put(channel, spannable);
    }

    public char getUserPrefix(final Channel channel) {
        final UserLevelEnum level = userLevelMap.get(channel);
        if (UserLevelEnum.OP.equals(level)) {
            return '@';
        } else if (UserLevelEnum.VOICE.equals(level)) {
            return '+';
        } else {
            return '\0';
        }
    }

    public UpdateableTreeSet<Channel> getChannels() {
        return userChannelInterface.getAllChannelsInUser(this);
    }

    public void putMode(final UserLevelEnum mode, final Channel channel) {
        userLevelMap.put(channel, mode);
        updateSpannableNick(channel);
    }

    public void processWhoMode(final String rawMode, final Channel channel) {
        UserLevelEnum mode = UserLevelEnum.NONE;
        // TODO - fix this up
        if (rawMode.contains("~")) {
            //mode = UserLevelEnum.OWNER;
            mode = UserLevelEnum.OP;
            channel.incrementOps();
        } else if (rawMode.contains("&")) {
            //mode = UserLevelEnum.SUPEROP;
            mode = UserLevelEnum.OP;
            channel.incrementOps();
        } else if (rawMode.contains("@")) {
            mode = UserLevelEnum.OP;
            channel.incrementOps();
        } else if (rawMode.contains("%")) {
            //mode = UserLevelEnum.HALFOP;
            mode = UserLevelEnum.VOICE;
            channel.incrementVoices();
        } else if (rawMode.contains("+")) {
            mode = UserLevelEnum.VOICE;
            channel.incrementVoices();
        }
        userLevelMap.put(channel, mode);
        updateSpannableNick(channel);
    }

    public String processModeChange(final Context context, final String sendingNick,
                                    final Channel channel, final String mode) {
        boolean addingMode = false;
        for (char character : mode.toCharArray()) {
            switch (character) {
                case '+':
                    addingMode = true;
                    break;
                case '-':
                    addingMode = false;
                    break;
                case 'o':
                    if (addingMode) {
                        if (userLevelMap.get(channel) == UserLevelEnum.VOICE) {
                            channel.decrementVoices();
                        }
                        channel.incrementOps();
                        channel.getUsers().update(this, ImmutableList.of(channel,
                                UserLevelEnum.OP));
                        break;
                    }
                case 'v':
                    if (addingMode && !userLevelMap.get(channel).equals(UserLevelEnum.OP)) {
                        channel.incrementVoices();
                        channel.getUsers().update(this,
                                ImmutableList.of(channel, UserLevelEnum.VOICE));
                        break;
                    }
                default:
                    if (!addingMode && (character == 'v' || character == 'o')) {
                        if (character == 'o') {
                            channel.decrementOps();
                        } else {
                            channel.decrementVoices();
                        }
                        channel.getUsers().update(this, ImmutableList.of(channel,
                                UserLevelEnum.NONE));
                    }
                    break;
            }
        }

        updateSpannableNick(channel);

        final String formattedSenderNick;
        final ChannelUser sendingUser = userChannelInterface.getUserIfExists(sendingNick);
        if (sendingUser == null) {
            formattedSenderNick = sendingNick;
        } else {
            formattedSenderNick = sendingUser.getPrettyNick(channel);
        }

        return String.format(context.getString(R.string.parser_mode_changed), mode,
                getColorfulNick(), formattedSenderNick);
    }

    @Override
    public void update() {
        // used for nick changes
    }

    @Override
    public void update(Object newValue) {
        if (newValue instanceof ImmutableList) {
            // ArrayList = mode change
            ImmutableList list = (ImmutableList) newValue;
            if (list.get(0) instanceof Channel && list.get(1) instanceof UserLevelEnum) {
                userLevelMap.put((Channel) list.get(0), (UserLevelEnum) list.get(1));
            }
        } else if (newValue instanceof Channel) {
            updateSpannableNick((Channel) newValue);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof ChannelUser) {
            ChannelUser us = ((ChannelUser) o);
            return us.getNick().equals(nick) && us.mServer.equals(mServer);
        } else {
            return false;
        }
    }

    @Override
    public void setChecked(boolean b) {
        mChecked = b;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        mChecked = !mChecked;
    }
}