/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.importexport.importer;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.importexport.common.FcrepoConstants.LAST_MODIFIED_DATE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.fcrepo.importexport.common.FcrepoConstants.REPOSITORY_NAMESPACE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.fcrepo.client.HeadBuilder;
import org.fcrepo.client.PutBuilder;
import org.fcrepo.importexport.common.AuthenticationRequiredRuntimeException;
import org.fcrepo.importexport.common.Config;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author escowles
 * @since 2016-09-06
 */
public class ImporterTest {

    private FcrepoClient client;
    private FcrepoClient.FcrepoClientBuilder clientBuilder;
    private Config binaryArgs;
    private Config binarySha256Args;
    private Config binarySha512Args;
    private Config noBinaryArgs;
    private Config externalResourceArgs;
    private Config containerArgs;
    private Config pairtreeArgs;
    private Config bagItArgs;
    private URI binaryURI;
    private URI binaryDescriptionURI;
    private URI binarySha256URI;
    private URI binarySha256DescriptionURI;
    private URI binarySha512URI;
    private URI binarySha512DescriptionURI;
    private URI externalResourceURI;
    private URI externalResourceDescriptionURI;
    private URI containerURI;
    private URI pairtreeURI;
    private URI finalContainerURI;
    private URI repositoryRootURI;
    private File binaryFilesDir;
    private File externalFilesDir;

    private FcrepoResponse conResponse;
    private PutBuilder binBuilder;
    private PutBuilder binSha256Builder;
    private PutBuilder binSha512Builder;
    private PutBuilder externalResourceBuilder;
    private GetBuilder getBuilder;
    private HeadBuilder headBuilder;
    private PutBuilder putBuilder;

    @Before
    public void setUp() throws Exception {
        binaryURI  = new URI("http://example.org:9999/rest/bin1");
        binaryDescriptionURI = new URI("http://example.org:9999/rest/bin1/fcr:metadata");
        binarySha256URI  = new URI("http://example.org:9999/rest/bin1-sha256");
        binarySha256DescriptionURI = new URI("http://example.org:9999/rest/bin1-sha256/fcr:metadata");
        binarySha512URI  = new URI("http://example.org:9999/rest/bin1-sha512");
        binarySha512DescriptionURI = new URI("http://example.org:9999/rest/bin1-sha512/fcr:metadata");

        externalResourceURI  = new URI("http://example.org:9999/rest/ext1");
        externalResourceDescriptionURI = new URI("http://example.org:9999/rest/ext1/fcr:metadata");
        containerURI = new URI("http://example.org:9999/rest/con1");
        repositoryRootURI = new URI("http://example.org:9999/rest");

        binaryFilesDir = new File("src/test/resources/sample/binary");
        binaryArgs = new Config();
        binaryArgs.setMode("import");
        binaryArgs.setBaseDirectory("src/test/resources/sample/binary");
        binaryArgs.setIncludeBinaries(true);
        binaryArgs.setRdfLanguage("application/ld+json");
        binaryArgs.setResource(new URI("http://example.org:9999/rest"));
        binaryArgs.setMap(new String[]{"http://localhost:8080/rest", "http://example.org:9999/rest"});

        binarySha256Args = new Config();
        binarySha256Args.setMode("import");
        binarySha256Args.setBaseDirectory("src/test/resources/sample/binary-sha256");
        binarySha256Args.setIncludeBinaries(true);
        binarySha256Args.setRdfLanguage("application/ld+json");
        binarySha256Args.setResource(new URI("http://example.org:9999/rest"));
        binarySha256Args.setMap(new String[]{"http://localhost:8080/rest", "http://example.org:9999/rest"});

        binarySha512Args = new Config();
        binarySha512Args.setMode("import");
        binarySha512Args.setBaseDirectory("src/test/resources/sample/binary-sha512");
        binarySha512Args.setIncludeBinaries(true);
        binarySha512Args.setRdfLanguage("application/ld+json");
        binarySha512Args.setResource(new URI("http://example.org:9999/rest"));
        binarySha512Args.setMap(new String[]{"http://localhost:8080/rest", "http://example.org:9999/rest"});

        noBinaryArgs = new Config();
        noBinaryArgs.setMode("import");
        noBinaryArgs.setBaseDirectory("src/test/resources/sample/binary");
        noBinaryArgs.setIncludeBinaries(false);
        noBinaryArgs.setRdfLanguage("application/ld+json");
        noBinaryArgs.setResource(new URI("http://example.org:9999/rest"));
        noBinaryArgs.setMap(new String[]{"http://localhost:8080/rest", "http://example.org:9999/rest"});

        externalFilesDir = new File("src/test/resources/sample/external");
        externalResourceArgs = new Config();
        externalResourceArgs.setMode("import");
        externalResourceArgs.setBaseDirectory("src/test/resources/sample/external");
        externalResourceArgs.setIncludeBinaries(true);
        externalResourceArgs.setRdfExtension(".jsonld");
        externalResourceArgs.setRdfLanguage("application/ld+json");
        externalResourceArgs.setResource(new URI("http://example.org:9999/rest"));
        externalResourceArgs.setMap(new String[]{"http://localhost:8080/rest", "http://example.org:9999/rest"});

        containerArgs = new Config();
        containerArgs.setMode("import");
        containerArgs.setBaseDirectory("src/test/resources/sample/container");
        containerArgs.setRdfLanguage("application/ld+json");
        containerArgs.setResource(new URI("http://example.org:9999/rest/con1"));
        containerArgs.setMap(new String[]{"http://localhost:8080/rest", "http://example.org:9999/rest"});

        pairtreeArgs = new Config();
        pairtreeArgs.setMode("import");
        pairtreeArgs.setBaseDirectory("src/test/resources/sample/pairtree");
        pairtreeArgs.setRdfLanguage("application/ld+json");
        pairtreeArgs.setResource(new URI("http://example.org:9999/rest"));
        pairtreeArgs.setMap(new String[]{"http://localhost:8080/rest", "http://example.org:9999/rest"});

        bagItArgs = new Config();
        bagItArgs.setMode("import");
        bagItArgs.setBaseDirectory("src/test/resources/sample/bagcorrupted");
        bagItArgs.setIncludeBinaries(true);
        bagItArgs.setRdfLanguage("application/ld+json");
        bagItArgs.setResource(new URI("http://example.org:9999/rest"));
        bagItArgs.setMap(new String[] { "http://localhost:8080/rest", "http://example.org:9999/rest" });
        bagItArgs.setBagProfile("default");
        bagItArgs.setUsername("tester");

        pairtreeURI = new URI("http://example.org:9999/rest/ab");
        finalContainerURI = new URI("http://example.org:9999/rest/ab/abc123");

        final List<URI> binLinks = Arrays.asList(binaryDescriptionURI);
        final List<URI> binSha256Links = Arrays.asList(binarySha256DescriptionURI);
        final List<URI> binSha512Links = Arrays.asList(binarySha512DescriptionURI);
        final List<URI> externalResourceLinks = Arrays.asList(externalResourceDescriptionURI);

        // mocks
        clientBuilder = mock(FcrepoClient.FcrepoClientBuilder.class);
        client = mock(FcrepoClient.class);
        when(clientBuilder.build()).thenReturn(client);

        // mock head interactions
        headBuilder = mock(HeadBuilder.class);
        final FcrepoResponse headResponse = mock(FcrepoResponse.class);
        when(client.head(isA(URI.class))).thenReturn(headBuilder);
        when(headBuilder.disableRedirects()).thenReturn(headBuilder);
        when(headBuilder.perform()).thenReturn(headResponse);
        when(headResponse.getStatusCode()).thenReturn(200);

        // mock get container/description interactions
        getBuilder = mock(GetBuilder.class);
        final FcrepoResponse getResponse = mock(FcrepoResponse.class);
        when(client.get(isA(URI.class))).thenReturn(getBuilder);
        when(getBuilder.accept(isA(String.class))).thenReturn(getBuilder);
        when(getBuilder.disableRedirects()).thenReturn(getBuilder);
        when(getBuilder.perform()).thenReturn(getResponse);
        when(getResponse.getStatusCode()).thenReturn(200);
        when(getResponse.getBody()).thenReturn(
                new ByteArrayInputStream(("{\"@type\":[\"" + REPOSITORY_NAMESPACE + "RepositoryRoot\"]}").getBytes()))
                .thenReturn(new ByteArrayInputStream("{}".getBytes()));

        // mock binary interactions
        binBuilder = mock(PutBuilder.class);
        final FcrepoResponse binResponse = mock(FcrepoResponse.class);
        when(client.put(eq(binaryURI))).thenReturn(binBuilder);
        when(binBuilder.body(isA(InputStream.class), isA(String.class))).thenReturn(binBuilder);
        when(binBuilder.digest(isA(String.class), eq("sha"))).thenReturn(binBuilder);
        when(binBuilder.filename(any())).thenReturn(binBuilder);
        when(binBuilder.ifUnmodifiedSince(any())).thenReturn(binBuilder);
        when(binBuilder.perform()).thenReturn(binResponse);
        when(binResponse.getStatusCode()).thenReturn(201);
        when(binResponse.getLinkHeaders(eq("describedby"))).thenReturn(binLinks);

        binSha256Builder = mock(PutBuilder.class);
        final FcrepoResponse binSha256Response = mock(FcrepoResponse.class);
        when(client.put(eq(binarySha256URI))).thenReturn(binSha256Builder);
        when(binSha256Builder.body(isA(InputStream.class), isA(String.class))).thenReturn(binSha256Builder);
        when(binSha256Builder.digest(isA(String.class), eq("sha256"))).thenReturn(binSha256Builder);
        when(binSha256Builder.filename(any())).thenReturn(binSha256Builder);
        when(binSha256Builder.ifUnmodifiedSince(any())).thenReturn(binSha256Builder);
        when(binSha256Builder.perform()).thenReturn(binSha256Response);
        when(binSha256Response.getStatusCode()).thenReturn(201);
        when(binSha256Response.getLinkHeaders(eq("describedby"))).thenReturn(binSha256Links);

        binSha512Builder = mock(PutBuilder.class);
        final FcrepoResponse binSha512Response = mock(FcrepoResponse.class);
        when(client.put(eq(binarySha512URI))).thenReturn(binSha512Builder);
        when(binSha512Builder.body(isA(InputStream.class), isA(String.class))).thenReturn(binSha512Builder);
        when(binSha512Builder.digest(isA(String.class), eq("sha512"))).thenReturn(binSha512Builder);
        when(binSha512Builder.filename(any())).thenReturn(binSha512Builder);
        when(binSha512Builder.ifUnmodifiedSince(any())).thenReturn(binSha512Builder);
        when(binSha512Builder.perform()).thenReturn(binSha512Response);
        when(binSha512Response.getStatusCode()).thenReturn(201);
        when(binSha512Response.getLinkHeaders(eq("describedby"))).thenReturn(binSha512Links);

        // mock external resource interactions
        externalResourceBuilder = mock(PutBuilder.class);
        final FcrepoResponse externalResourceResponse = mock(FcrepoResponse.class);
        when(client.put(eq(externalResourceURI))).thenReturn(externalResourceBuilder);
        when(externalResourceBuilder.body(isA(InputStream.class), isA(String.class)))
                .thenReturn(externalResourceBuilder);
        when(externalResourceBuilder.filename(any())).thenReturn(externalResourceBuilder);
        when(externalResourceBuilder.ifUnmodifiedSince(any())).thenReturn(externalResourceBuilder);
        when(externalResourceBuilder.perform()).thenReturn(externalResourceResponse);
        when(externalResourceResponse.getStatusCode()).thenReturn(201);
        when(externalResourceResponse.getLinkHeaders(eq("describedby"))).thenReturn(externalResourceLinks);


        // mock container/description interactions
        putBuilder = mock(PutBuilder.class);
        conResponse = mock(FcrepoResponse.class);
        when(client.put(eq(repositoryRootURI))).thenReturn(putBuilder);
        when(client.put(eq(containerURI))).thenReturn(putBuilder);
        when(client.put(eq(pairtreeURI))).thenReturn(putBuilder);
        when(client.put(eq(finalContainerURI))).thenReturn(putBuilder);
        when(client.put(eq(binaryDescriptionURI))).thenReturn(putBuilder);
        when(client.put(eq(binarySha256DescriptionURI))).thenReturn(putBuilder);
        when(client.put(eq(binarySha512DescriptionURI))).thenReturn(putBuilder);
        when(client.put(eq(externalResourceDescriptionURI))).thenReturn(putBuilder);
        when(putBuilder.body(isA(InputStream.class), isA(String.class))).thenReturn(putBuilder);
        when(putBuilder.preferLenient()).thenReturn(putBuilder);
        when(putBuilder.ifUnmodifiedSince(any())).thenReturn(putBuilder);
        when(putBuilder.perform()).thenReturn(conResponse);
        when(conResponse.getStatusCode()).thenReturn(201);
        when(conResponse.getLinkHeaders(eq("describedby"))).thenReturn(binLinks);
    }

    @Test
    public void testImportBinary() throws Exception {
        final Importer importer = new Importer(binaryArgs, clientBuilder);
        importer.run();
        verify(client).put(binaryURI);
        verify(binBuilder).digest(eq("2a6d6229e30f667c60d406f7bf44d834e52d11b7"), eq("sha"));
        verify(binBuilder).body(isA(InputStream.class), eq("application/x-www-form-urlencoded"));
        verify(client).put(binaryDescriptionURI);
    }

    @Test
    public void testImportBinarySha256() throws Exception {
        final Importer importer = new Importer(binarySha256Args, clientBuilder);
        importer.run();
        verify(client).put(binarySha256URI);
        verify(binSha256Builder).digest(eq("ff69f598bfef13ae4bf21ed6fd59fc3da5a9b4000f179d6e6d4049290db18a34"), eq("sha256"));
        verify(binSha256Builder).body(isA(InputStream.class), eq("application/x-www-form-urlencoded"));
        verify(client).put(binarySha256DescriptionURI);
    }

    @Test
    public void testImportBinarySha512() throws Exception {
        final Importer importer = new Importer(binarySha512Args, clientBuilder);
        importer.run();
        verify(client).put(binarySha512URI);
        verify(binSha512Builder).digest(eq("6943a10a0d92c126af2bdcc4b4e11b81e9e2cf61de4f8d1663bf419a31293f25372b7edb19bdf5fd967152ce035694effe1b1e370c5e651a7213f81f4ec55c37"), eq("sha512"));
        verify(binSha512Builder).body(isA(InputStream.class), eq("application/x-www-form-urlencoded"));
        verify(client).put(binarySha512DescriptionURI);
    }

    @Test
    public void testImportExternalResource() throws Exception {
        final Importer importer = new Importer(externalResourceArgs, clientBuilder);
        importer.run();
        verify(client).put(externalResourceURI);
        verify(client).put(externalResourceDescriptionURI);
    }

    @Test
    public void testImportWithoutBinaries() throws Exception {
        final Importer importer = new Importer(noBinaryArgs, clientBuilder);
        importer.run();
        verify(client, never()).put(binaryURI);
        verify(binBuilder, never()).body(isA(InputStream.class), eq("application/x-www-form-urlencoded"));
        verify(client, never()).put(binaryDescriptionURI);
    }

    @Test
    public void testImportContainer() throws Exception {
        final Importer importer = new Importer(containerArgs, clientBuilder);
        importer.run();
        verify(client).put(containerURI);
    }

    @Test
    public void testLegacyModeStripsLastModified() throws Exception {
        containerArgs.setLegacy(true);
        final ArgumentCaptor<InputStream> streamCapture = ArgumentCaptor.forClass(InputStream.class);
        final Importer importer = new Importer(containerArgs, clientBuilder);
        importer.run();

        verify(putBuilder).body(streamCapture.capture(), isA(String.class));
        final Model model = createDefaultModel();
        model.read(streamCapture.getValue(), "", "JSON-LD");
        final Graph graph = model.getGraph();
        assertFalse(graph.contains(Node.ANY, LAST_MODIFIED_DATE.asNode(), Node.ANY));
    }

    @Test
    public void testDefaultModeRetainsLastModified() throws Exception {
        final ArgumentCaptor<InputStream> streamCapture = ArgumentCaptor.forClass(InputStream.class);
        final Importer importer = new Importer(containerArgs, clientBuilder);
        importer.run();

        verify(putBuilder).body(streamCapture.capture(), isA(String.class));
        final Model model = createDefaultModel();
        model.read(streamCapture.getValue(), "", "JSON-LD");
        final Graph graph = model.getGraph();
        assertTrue(graph.contains(Node.ANY, LAST_MODIFIED_DATE.asNode(), Node.ANY));
    }

    @Test (expected = AuthenticationRequiredRuntimeException.class)
    public void testUnauthenticatedImportWhenAuthorizationIsRequired() throws FcrepoOperationFailedException {
        when(conResponse.getStatusCode()).thenReturn(401);
        final Importer importer = new Importer(containerArgs, clientBuilder);
        importer.run();
    }

    @Test
    public void testSkipPairtree() throws Exception {
        final Importer importer = new Importer(pairtreeArgs, clientBuilder);
        importer.run();
        verify(client).put(finalContainerURI);
        verify(client, never()).put(pairtreeURI);
    }

    @Test(expected = RuntimeException.class)
    public void testImportBagVerifyBinaryDigest() {
        // this fails Bag validation
        final Importer importer = new Importer(bagItArgs, clientBuilder);
        importer.run();
    }

    @Test(expected = RuntimeException.class)
    public void testImportBagFailsProfileValidation() throws URISyntaxException {
        bagItArgs = new Config();
        bagItArgs.setMode("import");
        bagItArgs.setBaseDirectory("src/test/resources/sample/baginvalid");
        bagItArgs.setIncludeBinaries(true);
        bagItArgs.setRdfLanguage("application/turtle");
        bagItArgs.setResource(new URI("http://example.org:9999/rest"));
        bagItArgs.setMap(new String[] { "http://localhost:8080/rest", "http://example.org:9999/rest" });
        bagItArgs.setBagProfile("default");
        bagItArgs.setUsername("tester");


        // this fails Bag Profile validation
        final Importer importer = new Importer(bagItArgs, clientBuilder);
        importer.run();
    }

    @Test
    public void testImportBagMultipleDigests() throws URISyntaxException, FcrepoOperationFailedException {
        final URI imageBinaryURI  = new URI("http://example.org:9999/rest/image0");
        final URI imageBinaryDescriptionURI = new URI("http://example.org:9999/rest/image0/fcr:metadata");

        final Config config = new Config();
        config.setMode("import");
        config.setBaseDirectory("src/test/resources/sample/bag-sha256");
        config.setIncludeBinaries(true);
        config.setRdfLanguage("application/ld+json");
        config.setResource(new URI("http://example.org:9999/rest/"));
        config.setMap(new String[] { "http://localhost:8080/rest/", "http://example.org:9999/rest/" });
        config.setBagProfile("default");
        config.setUsername("tester");

        final PutBuilder imageBuilder = mock(PutBuilder.class);
        final FcrepoResponse imageResponse = mock(FcrepoResponse.class);
        when(client.put(isA(URI.class))).thenReturn(imageBuilder);
        when(imageBuilder.body(isA(InputStream.class), isA(String.class))).thenReturn(imageBuilder);
        when(imageBuilder.digest(isA(String.class), isA(String.class))).thenReturn(imageBuilder);
        when(imageBuilder.filename(any())).thenReturn(imageBuilder);
        when(imageBuilder.ifUnmodifiedSince(any())).thenReturn(imageBuilder);
        when(imageBuilder.preferLenient()).thenReturn(imageBuilder);
        when(imageBuilder.perform()).thenReturn(imageResponse);
        when(imageResponse.getStatusCode()).thenReturn(201);
        when(imageResponse.getLinkHeaders(eq("describedby"))).thenReturn(
            Collections.singletonList(imageBinaryDescriptionURI));

        final Importer importer = new Importer(config, clientBuilder);
        importer.run();

        verify(client).put(imageBinaryURI);
        verify(imageBuilder, atLeastOnce()).digest(any(), eq("sha256"));
    }

    @Test
    public void testRepositoryRoot() throws FcrepoOperationFailedException {
        final Config config = new Config();
        config.setMode("import");
        final Importer importer = new Importer(config, clientBuilder);

        // when the uri is the repository root
        final URI rest = URI.create("http://example.org:2222/rest");
        mockGet(rest, REPOSITORY_NAMESPACE + "RepositoryRoot");
        assertEquals(rest, importer.findRepositoryRoot(rest));
    }

    @Test
    public void testRepositoryRootChild() throws FcrepoOperationFailedException {
        final Config config = new Config();
        config.setMode("import");
        final Importer importer = new Importer(config, clientBuilder);

        // when the uri is a child of the repository root
        final URI rest = URI.create("http://example.org:3333/rest");
        final URI child = URI.create("http://example.org:3333/rest/child");
        mockGet(rest, REPOSITORY_NAMESPACE + "RepositoryRoot");
        mockGet(child, REPOSITORY_NAMESPACE + "Resource");
        assertEquals(rest, importer.findRepositoryRoot(rest));
    }

    @Test
    public void testRepositoryRootFallback() throws FcrepoOperationFailedException {
        final Config config = new Config();
        config.setMode("import");
        final Importer importer = new Importer(config, clientBuilder);

        // when the uri has no path
        final URI dummy = URI.create("http://example.org:4444");
        assertEquals(dummy, importer.findRepositoryRoot(dummy));
    }

    private void mockGet(final URI uri, final String type) throws FcrepoOperationFailedException {
        final GetBuilder getBuilder = mock(GetBuilder.class);
        final FcrepoResponse getResponse = mock(FcrepoResponse.class);
        when(client.get(eq(uri))).thenReturn(getBuilder);
        when(getBuilder.accept(isA(String.class))).thenReturn(getBuilder);
        when(getBuilder.disableRedirects()).thenReturn(getBuilder);
        when(getBuilder.perform()).thenReturn(getResponse);
        when(getResponse.getStatusCode()).thenReturn(200);
        when(getResponse.getBody()).thenReturn(new ByteArrayInputStream((
            "<" + uri.toString() + "> a <" + type + "> .").getBytes()));
    }
}
