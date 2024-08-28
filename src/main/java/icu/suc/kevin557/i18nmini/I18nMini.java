package icu.suc.kevin557.i18nmini;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.google.common.collect.Lists;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class I18nMini extends JavaPlugin {

    private static final Pattern PATTERN = Pattern.compile("\\\\");

    private ProtocolManager manager;

    @Override
    public void onLoad() {
        manager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void onEnable() {
        manager.addPacketListener(new PacketAdapter(this, ListenerPriority.MONITOR, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                modifyItem(event.getPacket(), event.getPlayer().locale());
            }
        });
        manager.addPacketListener(new PacketAdapter(this, ListenerPriority.MONITOR, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                modifyItem(event.getPacket(), event.getPlayer().locale());
                modifyItemList(event.getPacket(), event.getPlayer().locale());
            }
        });
        manager.addPacketListener(new PacketAdapter(this, ListenerPriority.MONITOR, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                modifyDataValueCollection(event.getPacket(), event.getPlayer().locale());
            }
        });
        manager.addPacketListener(new PacketAdapter(this, ListenerPriority.MONITOR, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                modifyEquipments(event.getPacket(), event.getPlayer().locale());
            }
        });
    }

    @Override
    public void onDisable() {
        manager.removePacketListeners(this);
    }

    private static void modifyItem(PacketContainer packet, Locale locale) {
        StructureModifier<ItemStack> modifier = packet.getItemModifier();
        for (int i = 0; i < modifier.getValues().size(); i++) {
            modifier.writeSafely(i, modify(locale, modifier.readSafely(i).clone()));
        }
    }

    private static void modifyItemList(PacketContainer packet, Locale locale) {
        StructureModifier<List<ItemStack>> modifier = packet.getItemListModifier();
        for (int i = 0; i < modifier.getValues().size(); i++) {
            List<ItemStack> itemStacks = modifier.readSafely(i);
            itemStacks.replaceAll(itemStack -> modify(locale, itemStack.clone()));
            modifier.writeSafely(i, itemStacks);
        }
    }

    private static void modifyDataValueCollection(PacketContainer packet, Locale locale) {
        StructureModifier<List<WrappedDataValue>> modifier = packet.getDataValueCollectionModifier();
        for (int i = 0; i < modifier.getValues().size(); i++) {
            List<WrappedDataValue> wrappers = modifier.readSafely(i);
            for (WrappedDataValue wrapper : wrappers) {
                if (wrapper.getValue() instanceof ItemStack itemStack) {
                    wrapper.setValue(modify(locale, itemStack.clone()));
                }
            }
            modifier.writeSafely(i, wrappers);
        }
    }

    private static void modifyEquipments(PacketContainer packet, Locale locale) {
        StructureModifier<List<Pair<EnumWrappers.ItemSlot, ItemStack>>> modifier = packet.getSlotStackPairLists();
        for (int i = 0; i < modifier.getValues().size(); i++) {
            List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = modifier.readSafely(i);
            for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : pairs) {
                pair.setSecond(modify(locale, pair.getSecond().clone()));
            }
            modifier.writeSafely(i, pairs);
        }
    }

    private static ItemStack modify(Locale locale, ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasDisplayName()) {
                itemMeta.displayName(render(itemMeta.displayName(), locale));
            }
            if (itemMeta.hasLore()) {
                List<Component> lore = Lists.newArrayList();
                for (Component component : itemMeta.lore()) {
                    lore.add(render(component, locale));
                }
                itemMeta.lore(lore);
            }
            if (itemMeta instanceof BookMeta bookMeta) {
                bookMeta.title(render(bookMeta.title(), locale));
                bookMeta.author(render(bookMeta.author(), locale));
                List<Component> pages = bookMeta.pages();
                for (int i = 0; i < pages.size(); i++) {
                    bookMeta.page(i + 1, render(pages.get(i), locale));
                }
            }
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private static Component render(Component component, Locale locale) {
        return MiniMessage.miniMessage().deserialize(PATTERN.matcher(MiniMessage.miniMessage().serialize(GlobalTranslator.render(component, locale))).replaceAll(""));
    }
}
