package com.dousiyo.dpvptweaks.client.content;

import com.dousiyo.dpvptweaks.content.ContentEntry;
import com.dousiyo.dpvptweaks.content.ContentType;
import com.dousiyo.dpvptweaks.pvpstats.network.PvpStatsNetwork;
import com.dousiyo.dpvptweaks.pvpstats.network.c2s.RequestContentPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class ContentClient {
    private ContentClient() {}

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new ContentScreen(parent, ContentType.ANNOUNCEMENT));
        requestList(ContentType.ANNOUNCEMENT);
    }

    public static void requestList(ContentType type) {
        PvpStatsNetwork.CHANNEL.sendToServer(new RequestContentPacket(RequestContentPacket.Action.LIST, type, ""));
    }

    public static void requestDetail(ContentType type, String key) {
        PvpStatsNetwork.CHANNEL.sendToServer(new RequestContentPacket(RequestContentPacket.Action.DETAIL, type, key));
    }

    public static void receiveList(ContentType type, List<ContentEntry> entries) {
        if (Minecraft.getInstance().screen instanceof ContentScreen screen) screen.receiveList(type, entries);
    }

    public static void receiveDetail(ContentType type, String key, String title, String markdown) {
        if (Minecraft.getInstance().screen instanceof ContentScreen screen) screen.receiveDetail(type, key, title, markdown);
    }
}
