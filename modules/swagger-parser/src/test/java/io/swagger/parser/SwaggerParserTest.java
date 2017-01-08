package io.swagger.parser;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.*;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.*;


public class SwaggerParserTest {
    @Test
    public void testModelParameters() throws Exception {
        String yaml = "swagger: '2.0'\n" +
                "info:\n" +
                "  version: \"0.0.0\"\n" +
                "  title: test\n" +
                "paths:\n" +
                "  /foo:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "      - name: amount\n" +
                "        in: body\n" +
                "        schema:\n" +
                "          type: integer\n" +
                "          format: int64\n" +
                "          description: amount of money\n" +
                "          default: 1000\n" +
                "          maximum: 100000\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: ok";

        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.parse(yaml);

    }

    @Test
    public void testParseSharedPathParameters() throws Exception {
        String yaml =
                "swagger: '2.0'\n" +
                "info:\n" +
                "  version: \"0.0.0\"\n" +
                "  title: test\n" +
                "paths:\n" +
                "  /persons/{id}:\n" +
                "    parameters:\n" +
                "      - in: path\n" +
                "        name: id\n" +
                "        type: string\n" +
                "        required: true\n" +
                "        description: \"no\"\n" +
                "    get:\n" +
                "      parameters:\n" +
                "        - name: id\n" +
                "          in: path\n" +
                "          required: true\n" +
                "          type: string\n" +
                "          description: \"yes\"\n" +
                "        - name: name\n" +
                "          in: query\n" +
                "          type: string\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: ok\n";

        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.parse(yaml);
        List<Parameter> parameters = swagger.getPath("/persons/{id}").getGet().getParameters();
        assertTrue(parameters.size() == 2);
        Parameter id = parameters.get(0);
        assertEquals(id.getDescription(), "yes");
    }

    @Test
    public void testParseRefPathParameters() throws Exception {
        String yaml =
                "swagger: '2.0'\n" +
                "info:\n" +
                "  title: test\n" +
                "  version: '0.0.0'\n" +
                "parameters:\n" +
                "  report-id:\n" +
                "    name: id\n" +
                "    in: path\n" +
                "    type: string\n" +
                "    required: true\n" +
                "paths:\n" +
                "  /reports/{id}:\n" +
                "    parameters:\n" +
                "        - $ref: '#/parameters/report-id'\n" +
                "    put:\n" +
                "      parameters:\n" +
                "        - name: id\n" +
                "          in: body\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            $ref: '#/definitions/report'\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: ok\n" +
                "definitions:\n" +
                "  report:\n" +
                "    type: object\n" +
                "    properties:\n" +
                "      id:\n" +
                "        type: string\n" +
                "      name:\n" +
                "        type: string\n" +
                "    required:\n" +
                "    - id\n" +
                "    - name\n";
        SwaggerParser parser = new SwaggerParser();
        Swagger swagger = parser.parse(yaml);
    }

    @Test
    public void testUniqueParameters() throws Exception {
        String yaml =
                "swagger: '2.0'\n" +
                "info:\n" +
                "  title: test\n" +
                "  version: '0.0.0'\n" +
                "parameters:\n" +
                "  foo-id:\n" +
                "    name: id\n" +
                "    in: path\n" +
                "    type: string\n" +
                "    required: true\n" +
                "paths:\n" +
                "  /foos/{id}:\n" +
                "    parameters:\n" +
                "        - $ref: '#/parameters/foo-id'\n" +
                "    get:\n" +
                "      responses:\n" +
                "        200:\n" +
                "          schema:\n" +
                "            $ref: '#/definitions/foo'\n" +
                "    put:\n" +
                "      parameters:\n" +
                "        - name: foo\n" +
                "          in: body\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            $ref: '#/definitions/foo'\n" +
                "      responses:\n" +
                "        200:\n" +
                "          schema:\n" +
                "            $ref: '#/definitions/foo'\n" +
                "definitions:\n" +
                "  foo:\n" +
                "    type: object\n" +
                "    properties:\n" +
                "      id:\n" +
                "        type: string\n" +
                "    required:\n" +
                "      - id\n";
        SwaggerParser parser = new SwaggerParser();
        Swagger swagger = parser.parse(yaml);
        List<Parameter> parameters = swagger.getPath("/foos/{id}").getPut().getParameters();
        assertTrue(parameters.size() == 2);
    }

    @Test
    public void testLoadRelativeFileTree_Json() throws Exception {
        final Swagger swagger = doRelativeFileTest("src/test/resources/relative-file-references/json/parent.json");
        //Json.mapper().writerWithDefaultPrettyPrinter().writeValue(new File("resolved.json"), swagger);
    }

    @Test
    public void testLoadExternalNestedDefinitions() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/nested-references/b.yaml");

        Map<String, Model> definitions = swagger.getDefinitions();
        assertTrue(definitions.containsKey("x"));
        assertTrue(definitions.containsKey("y"));
        assertTrue(definitions.containsKey("z"));
        assertEquals(((RefModel) definitions.get("i")).get$ref(), "#/definitions/k");
    }

    @Test
    public void testPetstore() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo("src/test/resources/petstore.json", null, true);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());

        Swagger swagger = result.getSwagger();
        Map<String, Model> definitions = swagger.getDefinitions();
        Set<String> expectedDefinitions = new HashSet<String>();
        expectedDefinitions.add("User");
        expectedDefinitions.add("Category");
        expectedDefinitions.add("Pet");
        expectedDefinitions.add("Tag");
        expectedDefinitions.add("Order");
        expectedDefinitions.add("PetArray");
        assertEquals(definitions.keySet(), expectedDefinitions);

        Model petModel = definitions.get("Pet");
        Set<String> expectedPetProps = new HashSet<String>();
        expectedPetProps.add("id");
        expectedPetProps.add("category");
        expectedPetProps.add("name");
        expectedPetProps.add("photoUrls");
        expectedPetProps.add("tags");
        expectedPetProps.add("status");
        assertEquals(petModel.getProperties().keySet(), expectedPetProps);

        ArrayModel petArrayModel = (ArrayModel) definitions.get("PetArray");
        assertEquals(petArrayModel.getType(), "array");
        RefProperty refProp = (RefProperty) petArrayModel.getItems();
        assertEquals(refProp.get$ref(), "#/definitions/Pet");
        assertNull(petArrayModel.getProperties());
    }

    @Test
    public void testFileReferenceWithVendorExt() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/file-reference-with-vendor-ext/b.yaml");
        Map<String, Model> definitions = swagger.getDefinitions();
        assertTrue(definitions.get("z").getVendorExtensions().get("x-foo") instanceof ObjectNode);
        assertTrue(definitions.get("x").getVendorExtensions().get("x-foo") instanceof ObjectNode);
    }

    @Test
    public void testTroublesomeFile() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/troublesome.yaml");
    }

    @Test
    public void testLoadRelativeFileTree_Yaml() throws Exception {
        JsonToYamlFileDuplicator.duplicateFilesInYamlFormat("src/test/resources/relative-file-references/json",
                "src/test/resources/relative-file-references/yaml");
        final Swagger swagger = doRelativeFileTest("src/test/resources/relative-file-references/yaml/parent.yaml");
        assertNotNull(Yaml.mapper().writeValueAsString(swagger));
    }

    @Test
    public void testLoadRecursiveExternalDef() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/file-reference-to-recursive-defs/b.yaml");

        Map<String, Model> definitions = swagger.getDefinitions();
        assertEquals(((RefProperty) ((ArrayProperty) definitions.get("v").getProperties().get("children")).getItems()).get$ref(), "#/definitions/v");
        assertTrue(definitions.containsKey("y"));
        assertEquals(((RefProperty) ((ArrayProperty) definitions.get("x").getProperties().get("children")).getItems()).get$ref(), "#/definitions/y");
    }

    @Test
    public void testLoadNestedItemsReferences() {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo("src/test/resources/nested-items-references/b.yaml", null, true);
        Swagger swagger = result.getSwagger();
        Map<String, Model> definitions = swagger.getDefinitions();
        assertTrue(definitions.containsKey("z"));
        assertTrue(definitions.containsKey("w"));
    }

    @Test
    public void testIssue75() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue99.json");

        BodyParameter param = (BodyParameter)swagger.getPaths().get("/albums").getPost().getParameters().get(0);
        Model model = param.getSchema();

        assertNotNull(model);
        assertTrue(model instanceof ArrayModel);

        ArrayModel am = (ArrayModel) model;
        assertTrue(am.getItems() instanceof ByteArrayProperty);
        assertEquals(am.getItems().getFormat(), "byte");
    }

    @Test(enabled = false, description = "see https://github.com/swagger-api/swagger-parser/issues/337")
    public void testIssue62() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/fixtures/v2.0/json/resources/resourceWithLinkedDefinitions.json");

        assertNotNull(swagger.getPaths().get("/pets/{petId}").getGet());
    }

    @Test
    public void testIssue146() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue_146.yaml");
        assertNotNull(swagger);
        QueryParameter p = ((QueryParameter)swagger.getPaths().get("/checker").getGet().getParameters().get(0));
        StringProperty pp = (StringProperty)p.getItems();
        assertTrue("registration".equalsIgnoreCase(pp.getEnum().get(0)));
    }

    @Test(description="Test (path & form) parameter's required attribute")
    public void testParameterRequired() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/petstore.json");
        final List<Parameter> operationParams = swagger.getPath("/pet/{petId}").getPost().getParameters();
        
        final PathParameter pathParameter = (PathParameter) operationParams.get(0);
        Assert.assertTrue(pathParameter.getRequired());
        
        final FormParameter formParameter = (FormParameter) operationParams.get(1);
        Assert.assertFalse(formParameter.getRequired());
    }
    
    @Test(description="Test consumes and produces in top level and operation level")
    public void testConsumesAndProduces() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/consumes_and_produces");
        Assert.assertNotNull(swagger);

        // test consumes and produces at spec level
        Assert.assertEquals(swagger.getConsumes(), Arrays.asList("application/json"));
        Assert.assertEquals(swagger.getProduces(), Arrays.asList("application/xml"));
        
        // test consumes and produces at operation level
        Assert.assertEquals(swagger.getPath("/pets").getPost().getConsumes(), Arrays.asList("image/jpeg"));
        Assert.assertEquals(swagger.getPath("/pets").getPost().getProduces(), Arrays.asList("image/png"));
 
        // test empty consumes and produces at operation level
        Assert.assertEquals(swagger.getPath("/pets").getGet().getConsumes(), Collections.<String> emptyList());
        Assert.assertEquals(swagger.getPath("/pets").getGet().getProduces(), Collections.<String> emptyList());
 
        // test consumes and produces not defined at operation level
        Assert.assertNull(swagger.getPath("/pets").getPatch().getConsumes());
        Assert.assertNull(swagger.getPath("/pets").getPatch().getProduces());

    }

    @Test
    public void testIssue108() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/issue_108.json");

        assertNotNull(swagger);
    }

    @Test
    public void testIssue() {
        String yaml =
                "swagger: '2.0'\n" +
                "info:\n" +
                "  description: 'No description provided.'\n" +
                "  version: '2.0'\n" +
                "  title: 'My web service'\n" +
                "  x-endpoint-name: 'default'\n" +
                "paths:\n" +
                "  x-nothing: 'sorry not supported'\n" +
                "  /foo:\n" +
                "    x-something: 'yes, it is supported'\n" +
                "    get:\n" +
                "      parameters: []\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: 'Swagger API document for this service'\n" +
                "x-some-vendor:\n" +
                "  sometesting: 'bye!'";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Swagger swagger = result.getSwagger();

        assertEquals(swagger.getVendorExtensions().get("x-some-vendor"), Json.mapper().createObjectNode().put("sometesting", "bye!"));
        assertEquals(swagger.getPath("/foo").getVendorExtensions().get("x-something"), "yes, it is supported");
        assertTrue(result.getMessages().size() == 1);
        assertEquals(result.getMessages().get(0), "attribute paths.x-nothing is unsupported");
    }

    @Test
    public void testIssue292WithNoCollectionFormat() {
        String yaml =
                "swagger: '2.0'\n" +
                "info:\n" +
                "  version: '0.0.0'\n" +
                "  title: nada\n" +
                "paths:\n" +
                "  /persons:\n" +
                "    get:\n" +
                "      parameters:\n" +
                "      - name: testParam\n" +
                "        in: query\n" +
                "        type: array\n" +
                "        items:\n" +
                "          type: string\n" +
                "      responses:\n" +
                "        200:\n" +
                "          description: Successful response";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Swagger swagger = result.getSwagger();

        Parameter param = swagger.getPaths().get("/persons").getGet().getParameters().get(0);
        QueryParameter qp = (QueryParameter) param;
        assertNull(qp.getCollectionFormat());
    }

    @Test
    public void testIssue292WithCSVCollectionFormat() {
        String yaml =
                "swagger: '2.0'\n" +
                        "info:\n" +
                        "  version: '0.0.0'\n" +
                        "  title: nada\n" +
                        "paths:\n" +
                        "  /persons:\n" +
                        "    get:\n" +
                        "      parameters:\n" +
                        "      - name: testParam\n" +
                        "        in: query\n" +
                        "        type: array\n" +
                        "        items:\n" +
                        "          type: string\n" +
                        "        collectionFormat: csv\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: Successful response";
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(yaml);

        Swagger swagger = result.getSwagger();

        Parameter param = swagger.getPaths().get("/persons").getGet().getParameters().get(0);
        QueryParameter qp = (QueryParameter) param;
        assertEquals(qp.getCollectionFormat(), "csv");
    }

    @Test
    public void testIssue255() {
        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.read("objectExample.json");
        assertEquals(swagger.getDefinitions().get("SamplePayload").getExample(), "[{\"op\":\"replace\",\"path\":\"/s\",\"v\":\"w\"}]");
    }

    @Test
    public void testIssue286() {
        SwaggerParser parser = new SwaggerParser();

        Swagger swagger = parser.read("issue_286.yaml");
        Property response = swagger.getPath("/").getGet().getResponses().get("200").getSchema();
        assertTrue(response instanceof RefProperty);
        assertEquals(((RefProperty)response).getSimpleRef(), "issue_286_PetList");
        assertNotNull(swagger.getDefinitions().get("issue_286_Allergy"));
    }


    private Swagger doRelativeFileTest(String location) {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult readResult = parser.readWithInfo(location, null, true);
        if(readResult.getMessages().size() > 0) {
            Json.prettyPrint(readResult.getMessages());
        }
        final Swagger swagger = readResult.getSwagger();

        final Path path = swagger.getPath("/health");
        assertEquals(path.getClass(), Path.class); //we successfully converted the RefPath to a Path

        final List<Parameter> parameters = path.getParameters();
        assertParamDetails(parameters, 0, QueryParameter.class, "param1", "query");
        assertParamDetails(parameters, 1, HeaderParameter.class, "param2", "header");

        final Operation operation = path.getGet();
        final List<Parameter> operationParams = operation.getParameters();
        assertParamDetails(operationParams, 0, PathParameter.class, "param3", "path");
        assertParamDetails(operationParams, 1, HeaderParameter.class, "param4", "header");
        assertParamDetails(operationParams, 2, BodyParameter.class, "body", "body");
        final BodyParameter bodyParameter = (BodyParameter) operationParams.get(2);
        assertEquals(((RefModel) bodyParameter.getSchema()).get$ref(), "#/definitions/health");

        final Map<String, Response> responsesMap = operation.getResponses();

        assertResponse(swagger, responsesMap, "200", "Health information from the server", "#/definitions/health");
        assertResponse(swagger, responsesMap, "400", "Your request was not valid", "#/definitions/error");
        assertResponse(swagger, responsesMap, "500", "An unexpected error occur during processing", "#/definitions/error");

        final Map<String, Model> definitions = swagger.getDefinitions();
        final ModelImpl refInDefinitions = (ModelImpl) definitions.get("refInDefinitions");
        assertEquals(refInDefinitions.getDescription(), "The example model");
        expectedPropertiesInModel(refInDefinitions, "foo", "bar");

        final ArrayModel arrayModel = (ArrayModel) definitions.get("arrayModel");
        final RefProperty arrayModelItems = (RefProperty) arrayModel.getItems();
        assertEquals(arrayModelItems.get$ref(), "#/definitions/foo");

        final ModelImpl fooModel = (ModelImpl) definitions.get("foo");
        assertEquals(fooModel.getDescription(), "Just another model");
        expectedPropertiesInModel(fooModel, "hello", "world");

        final ComposedModel composedCat = (ComposedModel) definitions.get("composedCat");
        final ModelImpl child = (ModelImpl) composedCat.getChild();
        expectedPropertiesInModel(child, "huntingSkill", "prop2", "reflexes", "reflexMap");
        final ArrayProperty reflexes = (ArrayProperty) child.getProperties().get("reflexes");
        final RefProperty reflexItems = (RefProperty) reflexes.getItems();
        assertEquals(reflexItems.get$ref(), "#/definitions/reflex");
        assertTrue(definitions.containsKey(reflexItems.getSimpleRef()));

        final MapProperty reflexMap = (MapProperty) child.getProperties().get("reflexMap");
        final RefProperty reflexMapAdditionalProperties = (RefProperty) reflexMap.getAdditionalProperties();
        assertEquals(reflexMapAdditionalProperties.get$ref(), "#/definitions/reflex");

        assertEquals(composedCat.getInterfaces().size(), 2);
        assertEquals(composedCat.getInterfaces().get(0).get$ref(), "#/definitions/pet");
        assertEquals(composedCat.getInterfaces().get(1).get$ref(), "#/definitions/foo");

        return swagger;
    }

    private void expectedPropertiesInModel(ModelImpl model, String... expectedProperties) {
        assertEquals(model.getProperties().size(), expectedProperties.length);
        for (String expectedProperty : expectedProperties) {
            assertTrue(model.getProperties().containsKey(expectedProperty));
        }
    }

    private void assertResponse(Swagger swagger, Map<String, Response> responsesMap, String responseCode,
                                String expectedDescription, String expectedSchemaRef) {
        final Response response = responsesMap.get(responseCode);
        final RefProperty schema = (RefProperty) response.getSchema();
        assertEquals(response.getDescription(), expectedDescription);
        assertEquals(schema.getClass(), RefProperty.class);
        assertEquals(schema.get$ref(), expectedSchemaRef);
        assertTrue(swagger.getDefinitions().containsKey(schema.getSimpleRef()));
    }

    private void assertParamDetails(List<Parameter> parameters, int index, Class<?> expectedType,
                                    String expectedName, String expectedIn) {
        final Parameter param1 = parameters.get(index);
        assertEquals(param1.getClass(), expectedType);
        assertEquals(param1.getName(), expectedName);
        assertEquals(param1.getIn(), expectedIn);
    }

    @Test
    public void testNestedReferences() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/relative-file-references/json/parent.json");
        assertTrue(swagger.getDefinitions().containsKey("externalArray"));
        assertTrue(swagger.getDefinitions().containsKey("referencedByLocalArray"));
        assertTrue(swagger.getDefinitions().containsKey("externalObject"));
        assertTrue(swagger.getDefinitions().containsKey("referencedByLocalElement"));
        assertTrue(swagger.getDefinitions().containsKey("referencedBy"));
    }

    @Test
    public void testCodegenPetstore() {
        SwaggerParser parser = new SwaggerParser();
        final Swagger swagger = parser.read("src/test/resources/petstore-codegen.yaml");
        ModelImpl enumModel = (ModelImpl)swagger.getDefinitions().get("Enum_Test");
        assertNotNull(enumModel);
        Property enumProperty = enumModel.getProperties().get("enum_integer");
        assertNotNull(enumProperty);

        assertTrue(enumProperty instanceof IntegerProperty);
        IntegerProperty enumIntegerProperty = (IntegerProperty) enumProperty;
        List<Integer> integers = enumIntegerProperty.getEnum();
        assertEquals(integers.get(0), new Integer(1));
        assertEquals(integers.get(1), new Integer(-1));

        Operation getOrderOperation = swagger.getPath("/store/order/{orderId}").getGet();
        assertNotNull(getOrderOperation);
        Parameter orderId = getOrderOperation.getParameters().get(0);
        assertTrue(orderId instanceof PathParameter);
        PathParameter orderIdPathParam = (PathParameter) orderId;
        assertNotNull(orderIdPathParam.getMinimum());

        BigDecimal minimum = orderIdPathParam.getMinimum();
        assertEquals(minimum.toString(), "1.0");

        FormParameter formParam = (FormParameter)swagger.getPath("/fake").getPost().getParameters().get(3);

        assertEquals(formParam.getMinimum().toString(), "32.1");
    }
}
