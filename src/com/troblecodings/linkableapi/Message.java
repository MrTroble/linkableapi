package com.troblecodings.linkableapi;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;

public interface Message {

    default void message(final Player player, final String text, final Object... obj) {
        player.sendSystemMessage(getComponent(text, obj));
    }

    default MutableComponent getComponent(final String text, final Object... obj) {
        return MutableComponent.create(new TranslatableContents(text, null, obj));
    }

}
