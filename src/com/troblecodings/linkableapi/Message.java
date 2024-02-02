package com.troblecodings.linkableapi;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;

public interface Message {

    default void message(final PlayerEntity player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj));
    }

    default MutableText getComponent(final String text, final Object... obj) {
        return MutableText.of(new TranslatableTextContent(text, null, obj));
    }

}
