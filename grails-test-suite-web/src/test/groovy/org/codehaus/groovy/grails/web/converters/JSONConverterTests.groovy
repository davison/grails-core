package org.codehaus.groovy.grails.web.converters

import grails.artefact.Artefact
import grails.converters.JSON
import grails.persistence.Entity

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.servlet.mvc.HibernateProxy
import org.codehaus.groovy.grails.web.servlet.mvc.LazyInitializer
import org.codehaus.groovy.grails.web.util.StreamCharBuffer
import org.grails.web.converters.ConverterUtil;
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

/**
 * Tests for the JSON converter.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class JSONConverterTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getDomainClasses() {
        [Book]
    }

    @Override
    protected Collection<Class> getControllerClasses() {
        [JSONConverterController]
    }

    void testNullJSONValues() {
        def c = new JSONConverterController()
        c.testNullValues()

        assertEquals('{}', response.contentAsString)
    }

    void testJSONConverter() {
        def c = new JSONConverterController()
        c.test()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        assertEquals("""{"class":"${Book.name}","id":null,"author":"Stephen King","title":"The Stand"}""", response.contentAsString)
    }

    void testConvertErrors() {
        def c = new JSONConverterController()
        c.testErrors()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        def json = JSON.parse(response.contentAsString)
        def titleError = json.errors.find { it.field == 'title' }

        assertEquals "Property [title] of class [class ${Book.name}] cannot be null", titleError.message
        def authorError = json.errors.find { it.field == 'author' }
        assertEquals "Property [author] of class [class ${Book.name}] cannot be null", authorError.message
    }

    void testProxiedDomainClassWithJSONConverter() {

        def obj = new Book()
        obj.title = "The Stand"
        obj.author = "Stephen King"
        def c = new JSONConverterController()

        def hibernateInitializer = [getImplementation:{obj}] as LazyInitializer
        def proxy = [getHibernateLazyInitializer:{hibernateInitializer}] as HibernateProxy
        c.params.b = proxy

        c.testProxy()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        assertEquals("""{"class":"${Book.name}","id":null,"author":"Stephen King","title":"The Stand"}""", response.contentAsString)
    }

    void testJSONEnumConverting() {
        def enumClass = ga.classLoader.loadClass("Role")
        enumClass.metaClass.asType = {java.lang.Class clazz ->
            if (ConverterUtil.isConverterClass(clazz)) {
                return ConverterUtil.createConverter(clazz, delegate)
            }
            return ConverterUtil.invokeOriginalAsTypeMethod(delegate, clazz)
        }

        def enumInstance = enumClass.HEAD
        def c = new JSONConverterController()
        c.params.e = enumInstance
        c.testEnum()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        assertEquals('{"enumType":"Role","name":"HEAD"}', response.contentAsString)
    }
    
    // GRAILS-11513
    void testStringsWithQuotes() {
        def json = [quotedString: "I contain a \"Quote\"!", nonquotedString: "I don't!"] as JSON
        assertEquals('{"quotedString":"I contain a \\"Quote\\"!","nonquotedString":"I don\'t!"}', json.toString())
    }
    
    void testGStringsWithQuotes() {
        def json = [quotedString: "I contain a \"${'Quote'}\"!", nonquotedString: "I ${'don'}'t!"] as JSON
        assertEquals('{"quotedString":"I contain a \\"Quote\\"!","nonquotedString":"I don\'t!"}', json.toString())
    }
    
    void testStreamCharBufferWithQuotes() {
        def quotedBuffer = new StreamCharBuffer()
        quotedBuffer.writer << "I contain a \"Quote\"!"
        def nonquotedBuffer = new StreamCharBuffer()
        nonquotedBuffer.writer << "I don't!"
        def json = [quotedString: quotedBuffer, nonquotedString: nonquotedBuffer] as JSON
        assertEquals('{"quotedString":"I contain a \\"Quote\\"!","nonquotedString":"I don\'t!"}', json.toString())
    }
    
    void testObjectWithQuotes() {
        def json = [quotedString: new CustomCharSequence("I contain a \"Quote\"!"), nonquotedString: new CustomCharSequence("I don't!")] as JSON
        assertEquals('{"quotedString":"I contain a \\"Quote\\"!","nonquotedString":"I don\'t!"}', json.toString())
    }

    // GRAILS-11515
    void testJsonMultilineSerialization() {
        String multiLine = "first line \n second line"
        def object = [ line: multiLine ]
        def result = object as JSON
        
        assertEquals('{"line":"first line \\n second line"}', result.toString())
    }

    void onSetUp() {
        GroovySystem.metaClassRegistry.removeMetaClass Errors
        GroovySystem.metaClassRegistry.removeMetaClass BeanPropertyBindingResult

        gcl.parseClass '''
enum Role { HEAD, DISPATCHER, ADMIN }
'''
    }
}

@Artefact("Controller")
class JSONConverterController {
    def test = {
       def b = new Book(title:'The Stand', author:'Stephen King')
       render b as JSON
    }

    def testProxy = {
       render params.b as JSON
    }

    def testErrors = {
        def b = new Book()
        b.validate()
        render b.errors as JSON
    }

   def testEnum = {
       render params.e as JSON
   }

    def testNullValues = {
        def descriptors = [:]
        descriptors.put(null,null)
        render descriptors as JSON
    }
}

@Entity
class Book {
   Long id
   Long version
   String title
   String author
}

class CustomCharSequence implements CharSequence {
    String source
    
    CustomCharSequence(String source) {
        this.source = source
    }
    
    @Override
    public int length() {
        source.length()
    }

    @Override
    public char charAt(int index) {
        source.charAt(index)
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        source.subSequence(start, end)
    }
    
    @Override
    public String toString() {
        source
    }
}
