# OpenStreetMapin aukioloaikamuokkain

OpenStreetMapin aukiloaikojen määrittely on melko monimutkainen eikä se suoralta kädeltä taivu yksinkertaiseksi ja intuitiiviseksi käyttöliittymäksi.

Toisaalta määrittelystä käytetään monesti vain pientä osaa. Muokkain ottaa tämän huomioon ja yrittää piilottaa oudoimmat piirteet valikoihin, jolloin tien päällä tarvitsee useimmiten tehdä vain pieniä muutoksia valmiisiin mallipohjiin.

_Tämä dokumentaatio on alustava ja työn alla._

## Aukioloaikamuokkaimen käyttö

In a typical workflow the object you are editing will either already have an opening hours tag (opening_hours, service_times and collection_times) or you can re-apply the preset for the object to get an empty opening hours field. If you need to add the field manually and you are using Vespucci you can enter the key on the details page and then switch back to the form based tab to edit. If you believe that the opening hours tag should have been part of the preset, please open an issue for your editor.

If you have defined a default template (do this via the "Manage templates" menu item) it will be loaded automatically when the editor is started with an empty value. With the "Load template" function you can load any saved template and with the "Save template" menu you can save the current value as a template. You can define separate templates and defaults for the "opening_hours", "collection_times" and "service_times" tags. 

Voit luonnollisesti rakentaa aukioloajan (aoa) tyhjästä, mutta suosittelemme jonkin valmiin mallineen käyttämistä pohjana.

Kun muokkaimeen ladataan olemassa oleva arvo, se yritetään korjata automaattisesti aukioloaikamäärittelyn mukaiseksi. Jos tämä ei ole mahdollista, virheen summittainen paikka raa'assa aoa-arvossa korostetaan, ja voit yrittää korjata sen käsin. Suurinpiirtein neljänneksessä OpenStreetMapin tietokannan aoa-arvoista on ongelmia, ja alle kymmentä prosenttia ei voi korjata. Sivulla [OpeningHourParser] (https://github.com/simonpoole/OpeningHoursParser) kerrotaan tarkemmin mitä poikkeamia määrittelystä sallitaan.

### Päävalikkonappi

* __Lisää sääntö__: lisää uusi sääntö.
* __Add rule for holidays__: add a new rule for a holiday together with a state change.
* __Lisää sääntö 24/7__: lisää sääntö kohteelle, joka on aina auki; aukioloaikamäärittely ei tue alavalitsimien käyttämistä 24/7:n rinnalla, mutta tässä sallitaan korkeamman tason valitsimien lisääminen (esimerkiksi vuosijaksot).
* __Lataa malline__: lataa valmis malline.
* __Tallenna mallineeksi__: tallenna nykyinen aukioloaika-arvo mallineeksi tulevaa käyttöä varten.
* __Hallitse mallineita__: muokkaa (esim. muuta nimeä) ja poista valmiita mallineita. 
* __Päivitä__: jäsennä aukioloaika-arvo uudelleen.
* __Delete all__: remove all rules.

### Säännöt

Oletussäännöt lisätään _tavallisina_ sääntöinä, mikä tarkoittaa sitä, että ne syrjäyttävät aiemmat samoille päiville osuvat säännöt. Tällä voi olla merkitystä, kun määritetään laajennettuja aikoja – tyypillisesti tällöin on syytä muuttaa säännöt _lisääviksi_ valikkokohdasta _Näytä sääntötyyppi_.

#### Sääntövalikko

* __Lisää määre/kommentti__: muuta säännön vaikutusalaa, ja lisää valinnainen kommentti.
* __Lisää vapaapäivä__: lisää valitsin yleiselle tai koulujen vapaapäivälle.
* __Lisää aikaväli...__
    * __Aika–aika__: alkuaika ja loppuaika samana päivänä.
    * __Time - extended time__: a start time to an end time on the next day (example 26:00 is 02:00 (am) the next day).
    * __Vaiht. aika–aika__: vaihteleva alkuaika (aamuhämärä, iltahämärä, auringonnousu, auringonlasku) ja loppuaika samana päivänä.
    * __Vaiht. aika–laajennettu aika__: vaihteleva alkuaika ja loppuaika peräkkäisinä päivinä.
    * __Aika–vaiht. aika__: alkuaika ja vaihteleva loppuaika.
    * __Vaiht. aika–vaiht. aika__: vaihteleva alkuaika ja vaihteleva loppuaika.
    * __Aika__: tietty ajankohta.
    * __Aika, alkaen__: tietystä ajankohdasta eteenpäin.
    * __Vaihteleva aika__: vaihtelevana ajankohtana.
    * __Vaihteleva aika, alkaen__: vaihtelevasta ajankohdasta eteenpäin.
* __Lisää viikonpäiväjakso__: lisää viikonpäiviin perustuva valitsin.
* __Lisää päiväjakso...__
    * __Pvm–pvm__: alku- ja loppupäivämäärä (vuosi, kk, päivä).
    * __Vaihteleva pvm–pvm__: vaihteleva alkupäivämäärä (määrittelyssä on toistaiseksi vain _pääsiäinen_) ja loppupäivämäärä.
    * __Pvm–vaihteleva pvm__: alkupäivämäärä ja vaihteleva loppupäivämäärä.
    * __Vaihteleva pvm–vaihteleva pvm__: vaihteleva alku- ja loppupäivämäärä.
    * __Occurrence in month - occurrence in month__: from a start weekday occurrence in a month to the same.
    * __Occurrence in month - date__: from a start weekday occurrence in a month to a end date.
    * __Date - occurrence in month__: from a start date to an end weekday occurrence in a month.
    * __Occurrence in month - variable date__: from a start weekday occurrence in a month to an end variable date.
    * __Variable date - occurrence in month__: from a start variable date to an end weekday occurrence in a month.
    * __Date - open end__: from a start date onwards.
    * __Vaihteleva pvm, alkaen__: vaihtelevasta päivämäärästä eteenpäin.
    * __Occurrence in month - open end__: from a start weekday occurrence in a month onwards.
    * __Poikkeama...__: samat vaihtoehdot kuin edellä poikkeaman kanssa (harvoin käytetty).
* __Lisää vuosijakso__: lisää vuosiin perustuva valitsin.
* __Lisää viikkojakso__: lisää viikkonumeroihin perustuva valitsin.
* __Duplicate__: create a copy of this rule and insert it after the current position.
* __Näytä sääntötyyppi__: näytä säännön tyyppi ja/tai muuta sitä: _tavallinen_, _lisäävä_ ja _vara_ (ei ensimmäiselle säännölle).
* __Siirrä ylöspäin__: siirrä sääntö yhtä paikkaa ylemmäs (ei ensimmäiselle säännölle)
* __Siirrä alaspäin__: siirrä sääntö yhtä paikkaa alemmas.
* __Poista__: poista sääntö.

### Ajanjaksot

Jotta ajanjaksojen muokkaaminen olisi mahdollisimman helppoa, säätöpalkkien laajuus ja tarkkuus yritetään optimoida, kun olemassa oleva arvo ladataan. Uusien ajanjaksojen säätimissä alkuaika on 6:00, ja niissä on 15 minuutin jaotus. Asetuksia voi muuttaa valikon kautta.

#### Ajanjaksovalikko

* __Display time picker__: show a large number picker for selecting start and end time, on very small displays this is the preferred way of changing times.
* __15 minuutin jaotus__: käytä säätöpalkissa 15 minuutin tarkkuutta.
* __5 minuutin jaotus__: käytä säätöpalkissa 5 minuutin tarkkuutta.
* __1 minuutin jaotus__: käytä säätöpalkissa 1 minuutin tarkkuutta – todella vaikeaa puhelimessa.
* __Aloita keskiyöstä__: aseta säätöpalkin alkuaika keskiyöhön.
* __Näytä aikaväli__: näytä kenttä, johon asetetaan toistuva aikaväli minuutteina.
* __Poista__: poista ajanjakso.

