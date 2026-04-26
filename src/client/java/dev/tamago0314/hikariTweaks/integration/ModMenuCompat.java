package dev.tamago0314.hikariTweaks.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.tamago0314.hikariTweaks.gui.HikariTweaksConfigScreen;

public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HikariTweaksConfigScreen::new;
    }
}
