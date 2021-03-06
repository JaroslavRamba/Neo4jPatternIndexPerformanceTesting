import com.graphaware.test.performance.CacheConfiguration;
import com.graphaware.test.performance.CacheParameter;
import com.graphaware.test.performance.Parameter;
import com.graphaware.test.performance.PerformanceTest;
import com.graphaware.test.util.TestUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;

import java.util.*;

/**
 * Created by Jaroslav on 3/25/15.
 */
public class GetTrianglesWithSingleNodeOptPTest implements PerformanceTest {

    private SortedSet<String> triangleSet;
    private List<Map<String, Object>> optResults;
    private boolean writePermission = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public String shortName() {
        return "GetTrianglesWithSingleNodeOptPTest";
    }

    @Override
    public String longName() {
        return "Optimalization to get all triangles with querying " +
                "on single defined node id - where each node is set only once or never.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Parameter> parameters() {
        List<Parameter> result = new LinkedList<>();
        result.add(new CacheParameter("cache")); //no cache, low-level cache, high-level cache
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int dryRuns(Map<String, Object> params) {
        return ((CacheConfiguration) params.get("cache")).needsWarmup() ? 50 : 5;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int measuredRuns() {
        return 10;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> databaseParameters(Map<String, Object> params) {
        return ((CacheConfiguration) params.get("cache")).addToConfig(Collections.<String, String>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareDatabase(GraphDatabaseService database, final Map<String, Object> params) {
        //triangleSet = PerformanceTestHelper.getTriangleSetFromDatabase(database, "only-nodes");
        triangleSet = PerformanceTestHelper.getTriangleSetFromFile("ptt-only-nodes-original.txt", "only-nodes");
    }

    @Override
    public String getExistingDatabasePath() {
        return "testDb/graph1000-5000.db.zip";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RebuildDatabase rebuildDatabase() {
        return RebuildDatabase.AFTER_PARAM_CHANGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long run(final GraphDatabaseService database, Map<String, Object> params) {
        long time = 0;
        optResults = new LinkedList<Map<String, Object>>();

        time += TestUtils.time(new TestUtils.Timed() {
            @Override
            public void time() {
                Set<String> usedNodes = new HashSet<String>();
                for (String nodeId : triangleSet) {
                    nodeId = nodeId.split("_")[0];
                    if (!usedNodes.contains(nodeId)) {
                        Result result = database.execute(
                                "MATCH (a)--(b)--(c)--(a) " +
                                        "WHERE id(a)=" + nodeId + " " +
                                        "RETURN id(a), id(b), id(c) " +
                                        "UNION " +
                                        "MATCH (a)--(b)--(c)--(a) " +
                                        "WHERE id(b)=" + nodeId + " " +
                                        "RETURN id(a), id(b), id(c) " +
                                        "UNION " +
                                        "MATCH (a)--(b)--(c)--(a) " +
                                        "WHERE id(c)=" + nodeId + " " +
                                        "RETURN id(a), id(b), id(c)");
                        usedNodes.add(nodeId);
                        PerformanceTestHelper.prepareResults(writePermission, result, optResults);
                    }
                }
            }
        });

        writePermission = PerformanceTestHelper.saveResultToFile(writePermission, "ptt-single-node-opt", optResults);

        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean rebuildDatabase(Map<String, Object> params) {
        throw new UnsupportedOperationException("never needed, database rebuilt after every param change");
    }
}
