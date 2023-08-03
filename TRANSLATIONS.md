## Translating Vespucci

You can [translate this program into your language](https://app.transifex.com/openstreetmap/vespucci/) using the transifex project.

Default preset is [translated separately, also at transifex](https://app.transifex.com/openstreetmap/presets), see https://github.com/simonpoole/beautified-JOSM-preset for more information.

## Technical issues

The background/overlay names and descriptions are not translatable as iD translation system for them is non-standard. Somebody would need to code a workaround to fix that part.

NOTE: for building we assume for now that the github repository contains the current language files which the maintainers will commit now and then. If you want to add or change one of the existing default (English) strings you need to make a pull request against this repository.

### Setting up transifex locally and retrieving translations

#### Install

- get the transifex tx tool see http://support.transifex.com/customer/portal/articles/995605-installation.

- the repository already includes a suitable .tx directory with config file, given that it has become fairly complex you should use that.
 
#### Retrieving current translations
 
    tx pull -a
 
will retrieve all translations, skipping up to date translation files and creating new directories and strings.xml files for new languages.
 
If you are building with gradle the ``updateTranslations`` task will run tx for you (if tx is on your path).
 
