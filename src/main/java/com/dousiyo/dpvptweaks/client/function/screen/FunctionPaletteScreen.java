package com.dousiyo.dpvptweaks.client.function.screen;

import com.dousiyo.dpvptweaks.functionpalette.*;
import com.dousiyo.dpvptweaks.network.functionpalette.FunctionPaletteNetwork;
import com.dousiyo.dpvptweaks.network.functionpalette.c2s.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

public final class FunctionPaletteScreen extends Screen {
    private static final int ROW_HEIGHT=36, ROW_GAP=4;
    private List<FunctionPaletteAction> actions=List.of();
    private long revision; private boolean loading=true; private int scroll;
    public FunctionPaletteScreen(){ super(Component.translatable("gui.dpvptweaks.function_palette.title")); }
    @Override protected void init(){ rebuild(); FunctionPaletteNetwork.CHANNEL.sendToServer(new RequestFunctionListPacket()); }
    public void applyPaletteData(FunctionPaletteCategory data){ revision=data.revision(); actions=data.actions(); loading=false; scroll=0; rebuild(); }
    private void rebuild(){ clearWidgets(); int visible=Math.max(1,(height-90)/(ROW_HEIGHT+ROW_GAP)); scroll=Math.max(0,Math.min(scroll,Math.max(0,actions.size()-visible)));
        for(int i=0;i<visible && i+scroll<actions.size();i++){ var action=actions.get(i+scroll); addRenderableWidget(new ActionButton(width/2-150,38+i*(ROW_HEIGHT+ROW_GAP),300,ROW_HEIGHT,action)); }
        addRenderableWidget(Button.builder(Component.translatable("gui.dpvptweaks.function_palette.close"),b->onClose()).bounds(width/2-50,height-28,100,20).build());
    }
    private void select(FunctionPaletteAction action){
        if(!action.confirmation()){ execute(action); return; }
        minecraft.setScreen(new ConfirmFunctionScreen(this,action));
    }
    private void execute(FunctionPaletteAction action){ FunctionPaletteNetwork.CHANNEL.sendToServer(new RunFunctionPacket(action.id(),revision)); }
    @Override public boolean mouseScrolled(double x,double y,double delta){ if(delta!=0){ scroll+=(delta<0?1:-1); rebuild(); return true; } return super.mouseScrolled(x,y,delta); }
    @Override public void render(GuiGraphics g,int mx,int my,float pt){ renderBackground(g); g.drawCenteredString(font,title,width/2,15,0xffffff);
        if(loading) g.drawCenteredString(font,Component.translatable("gui.dpvptweaks.function_palette.status.loading"),width/2,height/2,0xaaaaaa);
        else if(actions.isEmpty()) g.drawCenteredString(font,Component.translatable("gui.dpvptweaks.function_palette.menu_empty"),width/2,height/2,0xaaaaaa);
        super.render(g,mx,my,pt); }
    @Override public boolean isPauseScreen(){ return false; }

    private final class ActionButton extends AbstractButton {
        private final FunctionPaletteAction action;
        ActionButton(int x,int y,int w,int h,FunctionPaletteAction a){ super(x,y,w,h,Component.literal(a.name())); action=a; }
        @Override public void onPress(){ select(action); }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float pt){ super.renderWidget(g,mx,my,pt); ResourceLocation id=ResourceLocation.tryParse(action.icon());
            var item=id==null?Items.COMMAND_BLOCK:BuiltInRegistries.ITEM.get(id); if(item==Items.AIR)item=Items.COMMAND_BLOCK; g.renderItem(new ItemStack(item),getX()+7,getY()+10);
            g.drawString(font,action.name(),getX()+29,getY()+7,0xffffff,false); String desc=font.plainSubstrByWidth(action.description(),getWidth()-38); g.drawString(font,desc,getX()+29,getY()+21,0xaaaaaa,false); }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out){ defaultButtonNarrationText(out); }
    }

    private static final class ConfirmFunctionScreen extends Screen {
        private final FunctionPaletteScreen parent; private final FunctionPaletteAction action;
        ConfirmFunctionScreen(FunctionPaletteScreen parent,FunctionPaletteAction action){ super(Component.translatable("gui.dpvptweaks.function_palette.confirm_title")); this.parent=parent; this.action=action; }
        @Override protected void init(){ addRenderableWidget(Button.builder(Component.translatable("gui.dpvptweaks.function_palette.execute"),b->{ parent.execute(action); minecraft.setScreen(parent); }).bounds(width/2-105,height/2+35,100,20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.dpvptweaks.function_palette.cancel"),b->minecraft.setScreen(parent)).bounds(width/2+5,height/2+35,100,20).build()); }
        @Override public void render(GuiGraphics g,int mx,int my,float pt){ renderBackground(g); g.drawCenteredString(font,title,width/2,height/2-45,0xffffff); g.drawCenteredString(font,action.name(),width/2,height/2-20,0xffffff);
            g.drawCenteredString(font,font.plainSubstrByWidth(action.description(),Math.min(360,width-30)),width/2,height/2,0xaaaaaa); super.render(g,mx,my,pt); }
        @Override public void onClose(){ minecraft.setScreen(parent); } @Override public boolean isPauseScreen(){ return false; }
    }
}
