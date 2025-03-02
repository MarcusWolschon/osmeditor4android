package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.ShadowWorkManager;
import de.blau.android.SignalUtils;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.listener.UploadListener;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@LargeTest
public class ApiTest {

    private static final String UPLOAD4_FIXTURE           = "upload4";
    private static final String CHANGESET5_FIXTURE        = "changeset5";
    private static final String CHANGESET4_FIXTURE        = "changeset4";
    private static final String UPLOAD3_FIXTURE           = "upload3";
    private static final String CHANGESET3_FIXTURE        = "changeset3";
    private static final String UPLOAD2_FIXTURE           = "upload2";
    private static final String CHANGESET2_FIXTURE        = "changeset2";
    private static final String CAPABILITIES2_FIXTURE     = "capabilities2";
    private static final String UPLOAD7_FIXTURE           = "upload7";
    private static final String PARTIALUPLOAD_FIXTURE     = "partialupload";
    private static final String CLOSE_CHANGESET_FIXTURE   = "close_changeset";
    private static final String UPLOAD1_FIXTURE           = "upload1";
    private static final String CHANGESET1_FIXTURE        = "changeset1";
    private static final String ELEMENTFETCH1_FIXTURE     = "elementfetch1";
    private static final String MULTIFETCH4_FIXTURE       = "multifetch4";
    private static final String MULTIFETCH3_FIXTURE       = "multifetch3";
    private static final String MULTIFETCH2_FIXTURE       = "multifetch2";
    private static final String MULTIFETCH1_FIXTURE       = "multifetch1";
    private static final String MULTIFETCH_LARGE1_FIXTURE = "multifetch_large1";
    private static final String MULTIFETCH_LARGE2_FIXTURE = "multifetch_large2";
    private static final String MULTIFETCH_LARGE3_FIXTURE = "multifetch_large3";
    private static final String MULTIFETCH_LARGE4_FIXTURE = "multifetch_large4";
    private static final String DOWNLOAD2_FIXTURE         = "download2";
    private static final String DOWNLOAD1_FIXTURE         = "download1";
    private static final String CAPABILITIES1_FIXTURE     = "capabilities1";
    private static final String TEST1_OSM_FIXTURE         = "test1.osm";

    static final String GENERATOR_NAME = "vesupucci test";

    public static final int TIMEOUT = 10;

    private MockWebServerPlus    mockServer = null;
    private AdvancedPrefDatabase prefDB     = null;
    private Main                 main       = null;
    private Preferences          prefs      = null;

    static class FailOnErrorHandler implements PostAsyncActionHandler {
        CountDownLatch signal;

        FailOnErrorHandler(@NonNull CountDownLatch signal) {
            this.signal = signal;
        }

        @Override
        public void onSuccess() {
            System.out.println("FailOnErrorHandler onSuccess");
            signal.countDown();
        }

        @Override
        public void onError(AsyncResult result) {
            fail("Expected success " + result.getCode() + " " + result.getMessage());
        }
    };

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        main = Robolectric.buildActivity(Main.class).create().resume().get();
        prefDB = new AdvancedPrefDatabase(main);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null));
        prefDB.selectAPI("Test");
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        Logic logic = App.getLogic();
        prefs = new Preferences(main);
        logic.setPrefs(prefs);
        logic.getMap().setPrefs(main, prefs);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
        prefs.close();
    }

    /**
     * Simple bounding box data download
     */
    @Test
    public void dataDownload() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(DOWNLOAD1_FIXTURE);
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984));

        // check that we have parsed and post processed relations correctly
        Relation r1 = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 1638705);
        Relation r2 = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        Relation parent = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2078158);
        assertTrue(r1.hasParentRelation(2078158));
        assertTrue(r2.hasParentRelation(2078158));
        assertNotNull(parent.getMember(r1));
        assertNotNull(parent.getMember(r2));
    }

    /**
     * Super ugly hack to get the looper to run
     */
    private void runLooper() {
        try {
            Thread.sleep(3000); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            // Ignore
        }
        shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * Download then download again and merge
     */
    @Test
    public void dataDownloadMerge() {
        dataDownload();

        // modify this node
        Logic logic = App.getLogic();
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        Map<String, String> tags = new TreeMap<>(n.getTags());
        tags.put(Tags.KEY_NAME, "dietikonBerg");
        try {
            logic.setTags(null, Node.NAME, 101792984L, tags);
        } catch (OsmIllegalOperationException e1) {
            fail(e1.getMessage());
        }

        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(DOWNLOAD2_FIXTURE);
        logic.downloadBox(main, new BoundingBox(8.3838500D, 47.3883000D, 8.3865200D, 47.3898500D), true, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        n.hasTag(Tags.KEY_NAME, "dietikonBerg");

        // test timestamp related stuff, no point in making a separate test
        Node t = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(t);
        assertTrue(t.hasTag("amenity", "toilets"));
        assertEquals(1429452889, t.getTimestamp()); // 2015-04-19T14:14:49Z
        assertNotEquals(Validator.OK,
                t.hasProblem(ApplicationProvider.getApplicationContext(), App.getDefaultValidator(ApplicationProvider.getApplicationContext())));
    }

    /**
     * Fetch multiple elements in one call
     */
    @Test
    public void dataDownloadMultiFetch() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(MULTIFETCH1_FIXTURE);
        mockServer.enqueue(MULTIFETCH2_FIXTURE);
        mockServer.enqueue(MULTIFETCH3_FIXTURE);
        mockServer.enqueue(MULTIFETCH4_FIXTURE);
        Logic logic = App.getLogic();

        List<Long> nodes = new ArrayList<>();
        nodes.add(Long.valueOf(416083528L));
        nodes.add(Long.valueOf(577098580L));
        nodes.add(Long.valueOf(577098578L));
        nodes.add(Long.valueOf(573380242L));
        nodes.add(Long.valueOf(577098597L));
        nodes.add(Long.valueOf(984783547L));
        nodes.add(Long.valueOf(984784083L));
        nodes.add(Long.valueOf(2190871496L));
        nodes.add(Long.valueOf(1623520413L));
        nodes.add(Long.valueOf(954564305L));
        nodes.add(Long.valueOf(990041213L));

        List<Long> ways = new ArrayList<>();
        ways.add(Long.valueOf(35479116L));
        ways.add(Long.valueOf(35479120L));

        logic.downloadElements(ApplicationProvider.getApplicationContext(), nodes, ways, null, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 573380242L));
        assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 35479116L));
    }

    /**
     * Fetch multiple elements in one call
     */
    @Test
    public void dataDownloadMultiFetch2() {

        Long[] ids = new Long[] { 33845733L, 33845734L, 33845736L, 33845737L, 33845738L, 33845740L, 33845741L, 33845784L, 101792984L, 275000525L, 275000526L,
                275000527L, 275000528L, 289987511L, 289987512L, 289987513L, 289987514L, 289987515L, 289987516L, 289987517L, 307847725L, 307847730L, 307847734L,
                416426210L, 416426211L, 416426212L, 416426213L, 416426214L, 416426216L, 416426217L, 416426220L, 416426221L, 416426224L, 416426243L, 416426254L,
                416426260L, 416426261L, 416426338L, 424598263L, 424598264L, 424598265L, 424598266L, 424598267L, 424598268L, 424598269L, 424598270L, 424598271L,
                424598272L, 424598273L, 424598274L, 424598275L, 424598276L, 424598277L, 424598278L, 424598279L, 424598280L, 424598281L, 424598282L, 424598284L,
                424598285L, 424598286L, 424598287L, 424598288L, 424598289L, 424598290L, 424598291L, 424598292L, 424598293L, 424598314L, 424598315L, 577098653L,
                577098660L, 577098663L, 599672189L, 599672190L, 599672196L, 599672197L, 599672198L, 599672199L, 599672200L, 599672211L, 599672212L, 599672213L,
                599672214L, 599672215L, 599672216L, 599672217L, 599672218L, 599672219L, 599672220L, 599672221L, 599672222L, 599672223L, 599672224L, 600181872L,
                600181873L, 600181874L, 600181877L, 600181925L, 600181926L, 600181937L, 600181945L, 600181947L, 600181973L, 600181974L, 631659160L, 631659172L,
                631659178L, 631659179L, 631659181L, 631659182L, 631659184L, 632742752L, 632742773L, 632742778L, 632742845L, 633468108L, 633468223L, 633468225L,
                633468228L, 633468231L, 633468234L, 633468237L, 633468240L, 633468248L, 633468250L, 633468399L, 633468402L, 633468404L, 633468409L, 633468411L,
                633468413L, 633468419L, 633468421L, 633468423L, 633468425L, 633468428L, 633468436L, 633469164L, 633469166L, 633486967L, 633486969L, 633486971L,
                634290753L, 634290755L, 634290757L, 634290759L, 635762205L, 635762208L, 635762213L, 635762214L, 635762215L, 635762216L, 635762217L, 635762218L,
                635762219L, 635762220L, 635762221L, 635762222L, 635762223L, 635762224L, 635870784L, 651647602L, 651647639L, 651647649L, 651647651L, 651647658L,
                651647661L, 651647665L, 651647679L, 651647682L, 651647689L, 651647691L, 651674760L, 651674763L, 651674766L, 651674815L, 651674849L, 651674854L,
                651674857L, 651674863L, 651674893L, 665075433L, 667102170L, 760592825L, 760592826L, 760592834L, 762032752L, 770132678L, 770132681L, 1027924944L,
                1116134144L, 1116134158L, 1116134199L, 1116134202L, 1116134204L, 1116134207L, 1116134223L, 1116134245L, 1116134249L, 1116134252L, 1116134254L,
                1116134266L, 1116134290L, 1116134295L, 1116134307L, 1116134309L, 1116134324L, 1116134330L, 1116134332L, 1116134334L, 1116134354L, 1116134368L,
                1116134402L, 1116134404L, 1116134417L, 1116134419L, 1116134429L, 1116134431L, 1116134435L, 1116134445L, 1116134453L, 1116134455L, 1116134461L,
                1116134469L, 1116134472L, 1116134473L, 1116134487L, 1116134489L, 1116134510L, 1116134511L, 1116134512L, 1116134524L, 1116134528L, 1116134560L,
                1201766157L, 1201766159L, 1201766170L, 1201766174L, 1201766177L, 1201766178L, 1201766179L, 1201766183L, 1201766189L, 1201766190L, 1201766192L,
                1201766193L, 1201766198L, 1201766205L, 1201766207L, 1201766208L, 1201766209L, 1201766213L, 1201766219L, 1201766220L, 1201766222L, 1201766223L,
                1201766224L, 1201766225L, 1201766226L, 1201766233L, 1201766235L, 1201766237L, 1201766238L, 1201766240L, 1201766241L, 1201766251L, 1201766253L,
                1201766255L, 1201766256L, 1201766258L, 1201766259L, 1201766261L, 1201766262L, 1382317430L, 1382317431L, 1617097168L, 1776775889L, 1776775891L,
                1776775892L, 1776775897L, 1776775902L, 1776775905L, 1776775927L, 1776775936L, 1776775942L, 1776775951L, 1776775962L, 1776775965L, 1776775968L,
                1962813695L, 1962813702L, 1962813703L, 1962813704L, 1962813705L, 1962813706L, 1962813707L, 1962813708L, 1962813709L, 1962813711L, 1962813714L,
                1962813715L, 1962813717L, 1962813719L, 1962813721L, 1962813723L, 1962813724L, 1962813726L, 1962813728L, 1962813730L, 1962813744L, 1962813747L,
                1962813748L, 1962813749L, 1962813750L, 1962813752L, 1962813753L, 1962813754L, 1962813755L, 1962813757L, 1962813758L, 1962813760L, 1962813762L,
                1962813764L, 1962813765L, 1962813776L, 1962813779L, 1962813781L, 1962813783L, 1962813785L, 1962813787L, 1962813789L, 1962813790L, 1962813791L,
                1962813792L, 1962813793L, 1962813794L, 1962813795L, 1962813796L, 1962813797L, 1962813798L, 1962813799L, 1962813800L, 1962813803L, 1962813804L,
                1962813815L, 1962813817L, 1962813818L, 1962813821L, 1962813823L, 1962813825L, 1962813827L, 1962813830L, 1962813832L, 1962813834L, 1962813835L,
                1962813836L, 1962813837L, 1962813839L, 1962813840L, 1962813842L, 1962813852L, 1962813857L, 1963111952L, 1963111969L, 1963144407L, 1963144411L,
                1963144420L, 1963144423L, 1963144427L, 1963251726L, 1963251729L, 1963251747L, 1963251749L, 1963251751L, 1963251753L, 1963251763L, 1963251766L,
                1963251768L, 1963251771L, 1963251774L, 1964060743L, 1964271369L, 1990776430L, 2160122160L, 2160122163L, 2160122164L, 2160122166L, 2160122169L,
                2160122171L, 2160122173L, 2160122175L, 2160122176L, 2160122178L, 2160122179L, 2160122181L, 2160122183L, 2160122184L, 2160122186L, 2160122187L,
                2160122189L, 2160122190L, 2160122192L, 2160122193L, 2160127195L, 2160127196L, 2160127198L, 2160127199L, 2160127201L, 2160127202L, 2160127204L,
                2160127205L, 2160127206L, 2164475886L, 2164475887L, 2164475888L, 2164475889L, 2202485255L, 2202485256L, 2202485291L, 2205498710L, 2205498715L,
                2205498723L, 2205498725L, 2205498735L, 2205498737L, 2205498746L, 2205498748L, 2205498756L, 2205498760L, 2205498779L, 2205498781L, 2205498783L,
                2205498785L, 2205498792L, 2205498794L, 2205498800L, 2205498802L, 2205579593L, 2205579594L, 2205579596L, 2205579598L, 2205579601L, 2205579603L,
                2205579605L, 2205579606L, 2205579608L, 2205579610L, 2205579611L, 2205579613L, 2205579615L, 2205579617L, 2205579621L, 2205579625L, 2205579627L,
                2205579628L, 2205579630L, 2205579634L, 2205579639L, 2205579653L, 2205579655L, 2205579657L, 2205579661L, 2205579662L, 2205579666L, 2205579668L,
                2205579672L, 2205579673L, 2205579675L, 2205579677L, 2205579678L, 2205579679L, 2205579680L, 2205579681L, 2205579682L, 2205579683L, 2205579684L,
                2205579685L, 2205579686L, 2205579688L, 2205579689L, 2205579691L, 2205579695L, 2205579698L, 2205579700L, 2205579702L, 2205579703L, 2205579705L,
                2205579714L, 2205579720L, 2205579722L, 2205579724L, 2205579725L, 2205579727L, 2205579729L, 2205579738L, 2205579740L, 2205579741L, 2205579743L,
                2205579745L, 2205579747L, 2205579749L, 2205579751L, 2205579753L, 2205579754L, 2205579757L, 2205579759L, 2205579760L, 2205579762L, 2205579764L,
                2205579766L, 2205579767L, 2205579768L, 2205579769L, 2205579770L, 2205579771L, 2205579772L, 2205579773L, 2205579774L, 2205579782L, 2205579784L,
                2205579786L, 2205579788L, 2205579790L, 2205579792L, 2205579794L, 2205579795L, 2205579797L, 2205579799L, 2205579801L, 2205579803L, 2205579804L,
                2205579806L, 2205579808L, 2205579810L, 2205579811L, 2205579813L, 2205579815L, 2205579817L, 2205579819L, 2205579821L, 2205579823L, 2205579824L,
                2205579826L, 2205579828L, 2205579830L, 2205579831L, 2205579833L, 2205579835L, 2205579837L, 2205579838L, 2205579840L, 2205579842L, 2205579843L,
                2205579845L, 2205579850L, 2205579851L, 2205579852L, 2205579853L, 2205579854L, 2205579855L, 2205579856L, 2205579857L, 2205579859L, 2205579861L,
                2205579863L, 2205579865L, 2205579867L, 2205579869L, 2205579871L, 2205579872L, 2205579874L, 2205579876L, 2205579878L, 2205579880L, 2205579882L,
                2205579884L, 2205579886L, 2205579888L, 2205579890L, 2205579892L, 2205579894L, 2205579896L, 2205579898L, 2205579900L, 2205579902L, 2205579904L,
                2205579906L, 2205579908L, 2205579910L, 2205579912L, 2205579922L, 2205579924L, 2205579926L, 2205579928L, 2205579930L, 2205579931L, 2205579932L,
                2205579933L, 2205579934L, 2205579935L, 2205579936L, 2205579937L, 2205579938L, 2205579939L, 2205579940L, 2205579941L, 2205579943L, 2205579945L,
                2205579947L, 2206392955L, 2206392959L, 2206392960L, 2206392962L, 2206392963L, 2206392965L, 2206392966L, 2206392967L, 2206392968L, 2206392969L,
                2206392970L, 2206392971L, 2206392972L, 2206392973L, 2206392974L, 2206392975L, 2206392976L, 2206392977L, 2206392978L, 2206392979L, 2206392980L,
                2206392981L, 2206392982L, 2206392983L, 2206392984L, 2206392985L, 2206392986L, 2206392987L, 2206392988L, 2206392989L, 2206392990L, 2206392991L,
                2206392992L, 2206392993L, 2206392994L, 2206392996L, 2206392998L, 2206393000L, 2206393001L, 2206393002L, 2206393003L, 2206393004L, 2206393005L,
                2206393010L, 2206393013L, 2206393014L, 2206393016L, 2206393017L, 2206393018L, 2206393019L, 2206393020L, 2206393021L, 2206393022L, 2206393023L,
                2206393024L, 2206393025L, 2206393026L, 2206393027L, 2206393028L, 2206393029L, 2206393030L, 2206393031L, 2206393032L, 2206393033L, 2206393034L,
                2206393035L, 2206393036L, 2206393037L, 2206393038L, 2206393039L, 2206393041L, 2206393043L, 2206393048L, 2206393051L, 2206393073L, 2209622070L,
                2209622111L, 2209622165L, 2210439786L, 2210439787L, 2210439788L, 2210439789L, 2210439816L, 2212634459L, 2212634468L, 2212634473L, 2212634477L,
                2212634482L, 2212634487L, 2212634490L, 2212634496L, 2212634502L, 2212634506L, 2212634510L, 2212634513L, 2212634518L, 2212634525L, 2212634530L,
                2212634534L, 2212634539L, 2212634549L, 2212634553L, 2212634558L, 2212634564L, 2212634569L, 2212634575L, 2212634581L, 2212634589L, 2212634594L,
                2212634600L, 2212634609L, 2212634615L, 2212634622L, 2212634629L, 2212634659L, 2212634669L, 2212634677L, 2212634684L, 2212634691L, 2212634699L,
                2212634708L, 2212634717L, 2212634725L, 2212634732L, 2212634740L, 2212634749L, 2212634755L, 2212634763L, 2212634544L, 33845718L, 33845724L };

        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(MULTIFETCH_LARGE1_FIXTURE);
        mockServer.enqueue(MULTIFETCH_LARGE2_FIXTURE);
        mockServer.enqueue(MULTIFETCH_LARGE3_FIXTURE);
        mockServer.enqueue(MULTIFETCH_LARGE4_FIXTURE);

        Logic logic = App.getLogic();

        logic.downloadElements(ApplicationProvider.getApplicationContext(), Arrays.asList(ids), null, null, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 1116134207L));
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 33845724L));
    }

    /**
     * Down load a Relation with members
     */
    @Test
    public void dataDownloadElement() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(ELEMENTFETCH1_FIXTURE);
        Logic logic = App.getLogic();

        logic.downloadElement(ApplicationProvider.getApplicationContext(), Relation.NAME, 2807173L, true, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Relation.NAME, 2807173L));
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 416426192L));
        assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 104364414L));
    }

    /**
     * Upload to changes (mock-)server
     */
    @Test
    public void dataUpload() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(TEST1_OSM_FIXTURE);
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        mockServer.enqueue(UPLOAD1_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        try {
            s.getCapabilities();
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(50000, s.getCachedCapabilities().getMaxElementsInChangeset());
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        assertEquals(4L, r.getOsmVersion());
    }

    /**
     * Upload a subset (just one) of changes (mock-)server
     */
    @Test
    public void dataUploadSelective() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(TEST1_OSM_FIXTURE);
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        mockServer.enqueue(PARTIALUPLOAD_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        try {
            s.getCapabilities();
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, Util.wrapInList(n));
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(50000, s.getCachedCapabilities().getMaxElementsInChangeset());
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        assertEquals(32, App.getDelegator().getApiElementCount());
    }

    /**
     * Upload a subset (just one) of changes (mock-)server, this time using logic so that we can test that the undo
     * storage is handle properly
     */
    @Test
    public void dataLogicUploadSelective() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(TEST1_OSM_FIXTURE);
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        // modify the element so it gets added to the undo storage
        assertTrue(logic.getUndo().getUndoElements(n).isEmpty());
        Map<String, String> tags = new HashMap<>(n.getTags());
        tags.put("test", "test");
        logic.setTags(main, n, tags);
        assertFalse(logic.getUndo().getUndoElements(n).isEmpty());
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        mockServer.enqueue(PARTIALUPLOAD_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final CountDownLatch signal2 = new CountDownLatch(1);
        UploadListener.UploadArguments arguments = new UploadListener.UploadArguments("TEST", "none", false, true, null, Util.wrapInList(n));
        logic.upload(main, arguments, new FailOnErrorHandler(signal2));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);

        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        assertEquals(32, App.getDelegator().getApiElementCount());
        // post upload the element should no longer be in the undo storage
        assertTrue(logic.getUndo().getUndoElements(n).isEmpty());
    }

    /**
     * Upload unchanged data (mock-)server
     */
    @Test
    public void dataUploadUnchanged() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(TEST1_OSM_FIXTURE);
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());
        n.setState(OsmElement.STATE_UNCHANGED);

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        mockServer.enqueue(UPLOAD7_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        try {
            s.getCapabilities();
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Upload to changes (mock-)server with reduced number of elements per changeset
     */
    @Test
    public void dataUploadSplit() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(TEST1_OSM_FIXTURE);
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);

        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());
        assertEquals(6L, n.getOsmVersion());

        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604);
        assertNotNull(w);
        assertEquals(OsmElement.STATE_MODIFIED, w.getState());
        assertEquals(18L, w.getOsmVersion());

        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        assertEquals(3L, r.getOsmVersion());

        mockServer.enqueue(CAPABILITIES2_FIXTURE);
        mockServer.enqueue(CHANGESET2_FIXTURE);
        mockServer.enqueue(UPLOAD2_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);
        mockServer.enqueue(CHANGESET3_FIXTURE);
        mockServer.enqueue(UPLOAD3_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);
        mockServer.enqueue(CHANGESET4_FIXTURE);
        mockServer.enqueue(UPLOAD4_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        try {
            s.getCapabilities();
            App.getDelegator().uploadToServer(s, "TEST", "none", false, false, null, null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(11, s.getCachedCapabilities().getMaxElementsInChangeset());
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604);
        assertNotNull(w);
        assertEquals(OsmElement.STATE_UNCHANGED, w.getState());
        assertEquals(19L, w.getOsmVersion());
        r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        assertEquals(4L, r.getOsmVersion());
    }

    /**
     * Retrieve a changeset by id
     */
    @Test
    public void getChangeset() {
        mockServer.enqueue(CHANGESET5_FIXTURE);
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        Changeset cs = s.getChangeset(1234567);
        assertNotNull(cs);
        assertEquals(120631739L, cs.getOsmId());
        assertEquals(21, cs.getChanges());
        assertNotNull(cs.getTags());
        assertEquals("swisstopo SWISSIMAGE;Mapillary Images;KartaView Images", cs.getTags().get("imagery_used"));
    }

    /**
     * Update a changeset
     */
    @Test
    public void updateChangeset() {
        mockServer.enqueue(CHANGESET5_FIXTURE);
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        try {
            Changeset cs = s.updateChangeset(1234567, "ignored", "ignored", "ignored", null);
            assertNotNull(cs);
            assertEquals(120631739L, cs.getOsmId());
            assertNotNull(cs.getTags());
            assertEquals("swisstopo SWISSIMAGE;Mapillary Images;KartaView Images", cs.getTags().get("imagery_used"));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * open an existing changeset
     */
    @Test
    public void openExistingChangeset() {
        mockServer.enqueue(CHANGESET5_FIXTURE);
        mockServer.enqueue(CHANGESET5_FIXTURE);
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        s.setOpenChangeset(123456789);
        try {
            s.openChangeset(false, "ignored", "ignored", "ignored", null);
            assertEquals(123456789, s.getOpenChangeset()); // still open
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try {
            mockServer.takeRequest();
            RecordedRequest request = mockServer.takeRequest();
            assertEquals("/api/0.6/changeset/123456789", request.getPath());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    /**
     * replace an existing changeset
     */
    @Test
    public void replaceExistingChangeset() {
        mockServer.enqueue(CHANGESET5_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        s.setOpenChangeset(123456789);
        try {
            s.openChangeset(true, "ignored", "ignored", "ignored", null);
            assertEquals(1234567, s.getOpenChangeset()); // new id
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try {
            mockServer.takeRequest();
            mockServer.takeRequest();
            RecordedRequest request = mockServer.takeRequest();
            assertEquals("/api/0.6/changeset/create", request.getPath());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
