package io.github.gaol.git_rev_missing;

import org.junit.Assert;
import org.junit.Test;

public class RepoServiceTest {

    @Test
    public void testRemovePatchLocation () {
        String patch1 = "@@ -191,6 +191,9 @@ public class AsyncContextImpl implements AsyncContext {\n" +
                "                 Connectors.executeRootHandler(new HttpHandler() {\n" +
                "                     @Override\n" +
                "                     public void handleRequest(final HttpServerExchange exchange) throws Exception {\n" +
                "+                        ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);\n" +
                "+                        src.setServletRequest(servletRequest);\n" +
                "+                        src.setServletResponse(servletResponse);\n" +
                "                         servletDispatcher.dispatchToPath(exchange, pathInfo, DispatcherType.ASYNC);\n" +
                "                     }\n" +
                "                 }, exchange);\n";

        String expected = " public class AsyncContextImpl implements AsyncContext {\n" +
                "                 Connectors.executeRootHandler(new HttpHandler() {\n" +
                "                     @Override\n" +
                "                     public void handleRequest(final HttpServerExchange exchange) throws Exception {\n" +
                "+                        ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);\n" +
                "+                        src.setServletRequest(servletRequest);\n" +
                "+                        src.setServletResponse(servletResponse);\n" +
                "                         servletDispatcher.dispatchToPath(exchange, pathInfo, DispatcherType.ASYNC);\n" +
                "                     }\n" +
                "                 }, exchange);\n";


        Assert.assertEquals(expected, RepoService.trimPatchLocation(patch1));
    }

    @Test
    public void testMessageSimilar() {
        String m1 = "Fix issue with 100-continue and h2";
        String m2 = "[UNDERTOW-1657] Fix issue with 100-continue and h2";
        double similar = RepoService.similarness(m1, m2);
        Assert.assertTrue(similar > 0.7d);
        Assert.assertEquals(m1, RepoService.commitMessageTrim(m2));

        m1 = "[JBEAP-18580] Treat whitespace as illegal in header field-name";
        m2 = "[JBEAP-18580][UNDERTOW-1774] Treat whitespace as illegal in header field-name";
        similar = RepoService.similarness(m1, m2);
        Assert.assertTrue(similar > 0.7d);
        Assert.assertEquals(RepoService.commitMessageTrim(m1), RepoService.commitMessageTrim(m2));

        m1 = "[JBEAP-19266] NullPointerException when calling the AJP port with invalid request";
        m2 = "[UNDERTOW-1709][JBEAP-19266] NullPointerException when calling the AJP port with invalid request";
        similar = RepoService.similarness(m1, m2);
        Assert.assertTrue(similar > 0.7d);
        Assert.assertEquals(RepoService.commitMessageTrim(m1), RepoService.commitMessageTrim(m2));

    }

    @Test
    public void testVersionCompare() {
        Assert.assertTrue(Main.versionLarger("2.0.31", "2.0.30"));
        Assert.assertTrue(Main.versionLarger("2.1.0", "2.0.31"));
        Assert.assertTrue(Main.versionLarger("2.1", "2.0.31"));
        Assert.assertTrue(Main.versionLarger("2.1.0", "2.0"));
        Assert.assertTrue(Main.versionLarger("2.0.31.SP1", "2.0.30.SP4"));
        Assert.assertTrue(Main.versionLarger("2.0.31.SP4", "2.0.31.SP1"));
        Assert.assertTrue(Main.versionLarger("2.0.11", "2.0.9"));
        Assert.assertTrue(Main.versionLarger("5.0.3.Final-redhat-00007", "5.0.3.Final-redhat-00005"));
        Assert.assertTrue(Main.versionLarger("5.0.3.Final-redhat-00010", "5.0.3.Final-redhat-00007"));
    }

}
