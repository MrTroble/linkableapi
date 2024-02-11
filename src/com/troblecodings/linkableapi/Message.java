package com.troblecodings.linkableapi;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.text.TranslatableText;

public interface Message {

    default void message(final PlayerEntity player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj));
    }

    default BaseText getComponent(final String text, final Object... obj) {
        return new TranslatableText(text, obj);
    }

}
