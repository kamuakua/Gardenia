package cn.gardenia.client.module.misc;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;

public class LanguageModule extends Module {
    private final Setting<String> language = new Setting<>("Language", "Client display language", "简体中文", new String[]{"简体中文", "English"});

    public LanguageModule() {
        super("Language", "Switches client display language", Category.MISC);
        addSetting(language);
    }

    @Override
    public void onEnable() {
        setEnabled(false);
    }

    public String getLanguage() {
        return language.getString();
    }
}
