## Translating Vespucci

You can translate this program into your language using [transifex](https://www.transifex.com/projects/p/vespucci/) project.

## Technical issues

The background/overlay names and descriptions are not translatable as iD traslation system for them is non-standard. Somebody would need to code a workaround to fix that part.

NOTE: for building we assume for now that the github repository contains the current language files which the maintainers will commit now and then. If you want to add or change one of the existing default (English) strings you need to make a pull request against this repository.

### Setting up transifex locally and retrieving translations

#### Install

- get the transifex tx tool see http://support.transifex.com/customer/portal/articles/995605-installation.

- the repository already includes a suitable .tx directory with config file, given that it has become fairly complex you should use that, its contents are included at the end of this document.
 
#### Retrieving current translations
 
    tx pull -a
 
will retrieve all translations, skipping up to date translation files and creating new directories and strings.xml files for new languages.
 
If you are building with gradle the ``updateTranslations`` task will run tx for you (if tx is on your path).
 
 
#### transifex configuration file

    [main]
    host = https://www.transifex.com
    
    [vespucci.main]
    file_filter = src/main/res/values-<lang>/strings.xml
    lang_map = cs: cs-rCZ, zh_TW: zh-rTW, pt_BR: pt-rBR, zh-Hans: zh-rCN, sv_SE: sv-rSE, id: in 
    source_file = src/main/res/values/strings.xml
    source_lang = en
    minimum_perc = 5
    
    [vespucci.addresstagsxml]
    file_filter = src/main/res/values-<lang>/addresstags.xml
    lang_map = cs: cs-rCZ, zh_TW: zh-rTW, pt_BR: pt-rBR, zh-Hans: zh-rCN, sv_SE: sv-rSE, id: in
    source_file = src/main/res/values/addresstags.xml
    source_lang = en
    minimum_perc = 100
    
    [vespucci.bugfilterxml]
    file_filter = src/main/res/values-<lang>/bugfilter.xml
    lang_map = cs: cs-rCZ, zh_TW: zh-rTW, pt_BR: pt-rBR, zh-Hans: zh-rCN, sv_SE: sv-rSE, id: in
    source_file = src/main/res/values/bugfilter.xml
    source_lang = en
    minimum_perc = 100
    
    [vespucci.scalexml]
    file_filter = src/main/res/values-<lang>/scale.xml
    lang_map = cs: cs-rCZ, zh_TW: zh-rTW, pt_BR: pt-rBR, zh-Hans: zh-rCN, sv_SE: sv-rSE, id: in
    source_file = src/main/res/values/scale.xml
    source_lang = en
    minimum_perc = 100
    
    [vespucci.voicexml]
    file_filter = src/main/res/values-<lang>/voice.xml
    lang_map = cs: cs-rCZ, zh_TW: zh-rTW, pt_BR: pt-rBR, zh-Hans: zh-rCN, sv_SE: sv-rSE, id: in
    source_file = src/main/res/values/voice.xml
    source_lang = en
    minimum_perc = 100
    
    [vespucci.introductionmd]
    file_filter = documentation/docs/help/<lang>/Introduction.md
    lang_map = cs: cs-rCZ, zh_TW: zh-rTW, pt_BR: pt-rBR,zh-Hans: zh-rCN, sv_SE: sv-rSE, id: in 
    source_file = documentation/docs/help/en/Introduction.md
    source_lang = en
    minimum_perc = 5
    
    [presets.presetpot]
    type = PO
    file_filter = src/main/assets/preset_<lang>.po
    lang_map = cs: cs-rCZ, zh_TW: zh-rTW, pt_BR: pt-rBR, zh-Hans: zh-rCN, sv_SE: sv-rSE, id: in
    source_lang = en
    minimum_perc = 5

