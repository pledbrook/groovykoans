package org.groovykoans.koan09

/**
 * Koan09 - Meta-programming (Meta Object Protocol)
 *
 * Reading list:
 *   * http://mrhaki.blogspot.com/2009/10/groovy-goodness-expando-as-dynamic-bean.html
 *   * http://mrhaki.blogspot.com/2009/11/groovy-goodness-intercept-methods-with.html
 *   * http://groovy.codehaus.org/Closures#Closures-thisowneranddelegate
 *   * http://stackoverflow.com/questions/8120949/what-does-delegate-mean-in-groovy/8121750#8121750
 *   * http://mrhaki.blogspot.com/2009/12/groovy-goodness-adding-or-overriding.html
 *   * http://www.codinghorror.com/blog/2007/02/why-cant-programmers-program.html,
 *
 */
class Koan09 extends GroovyTestCase {

    void test01_Expando() {
        // So far we haven't dealt with the dynamic nature of Groovy. What does dynamic mean? It means
        // that you can change classes' behavior during runtime. Let's take the Groovy Expando class as an example.
        // http://mrhaki.blogspot.com/2009/10/groovy-goodness-expando-as-dynamic-bean.html

        // Create an Expando class and dynamically create a 'firstName' property set with some value. Also
        // add a sayHello() method that returns "Hello from ${firstName}"
        def expando
        // ------------ START EDITING HERE ----------------------
        expando = new Expando(firstName: 'Frank')
        expando.sayHello = {->
            "Hello from ${firstName}"
        }
        // ------------ STOP EDITING HERE  ----------------------

        assertNotNull('firstName property was not found', expando?.firstName)
        assertEquals("Hello from ${expando.firstName}", expando.sayHello())
    }

    void test02_GroovyInterceptors() {
        // Groovy's dynamic nature allows you to add custom interceptors to all method invocations. This is very similar
        // to AOP and can prove to be very useful, if used with caution.
        // Read here: http://mrhaki.blogspot.com/2009/11/groovy-goodness-intercept-methods-with.html

        // sensitiveService.nukeCity(username, city) allows you to nuke cities. But only if you're an 'admin'.
        // Using the NukeInterceptor, make sure that only admin is allowed to run this service.
        def proxy
        // ------------ START EDITING HERE ----------------------
        proxy = ProxyMetaClass.getInstance(SensitiveService)
        proxy.interceptor = new NukeInterceptor()
        // ------------ STOP EDITING HERE  ----------------------

        proxy.use {
            def sensitiveService = new SensitiveService()
            sensitiveService.nukeCity('jojo', 'Hogsmeade')
            assertEquals(sensitiveService.numberOfNukes, 0)
            sensitiveService.nukeCity('admin', 'Hogsmeade')
            assertEquals(sensitiveService.numberOfNukes, 1)
        }

    }

    void test03_ThisDelegateAndOwner() {
        // Let's get to know the difference between this, owner, and delegate.
        // Some reading is available here: http://groovy.codehaus.org/Closures#Closures-thisowneranddelegate

        // In Java, we only have the 'this' keyword. It returns the current instance. Groovy does exactly the same.
        def whatIsThisEqual
        // ------------ START EDITING HERE ----------------------
        whatIsThisEqual = 'org.groovykoans.koan09.Koan09'
        // ------------ STOP EDITING HERE  ----------------------
        assertEquals(this.class.name, whatIsThisEqual)

        def outerClosure = {->
            println 'hello from first closure'
            def innerClosure = {->
                println 'hello from second closure'
            }
        }
        // The owner is the same thing as 'this'. Unless you are surrounded by a Closure, in which case the Closure is
        // your owner.
        def firstClosureOwner, secondClosureOwner
        // ------------ START EDITING HERE ----------------------
        firstClosureOwner = 'org.groovykoans.koan09.Koan09'
        secondClosureOwner = 'org.groovykoans.koan09.Koan09$_test03_ThisDelegateAndOwner_closure3'
        // ------------ STOP EDITING HERE  ----------------------
        assertEquals(outerClosure.owner.class.name, firstClosureOwner)
        assertEquals(outerClosure().owner.class.name, secondClosureOwner)

        // And finally, delegate is the same as owner, only that it can be modified by an external script.
        // Changing the delegate allows you to change the 'context' in which the closure is run. It may
        // seem a bit artificial, but when we introduce Groovy Builders, you'll see how powerful this feature is.

        // First, let's revisit closures. In Koan04, we mentioned that a closure has parameters, an implicit variable,
        // and free variables. What are free variables? They're the variables that are 'inherited' into the closure
        // from the environment the closure was defined in. For example:
        def calculateWeight = { mass ->
            mass * gravity   // gravity is a free variable, mass is a parameter
        }
        calculateWeight.resolveStrategy = Closure.DELEGATE_ONLY

        // Using delegates, we can change the free variables!
        calculateWeight.delegate = new ConstantsOnMoon()
        def weightOnMoon = calculateWeight(10)

        calculateWeight.delegate = new ConstantsOnEarth()
        def weightOnEarth = calculateWeight(10)

        // Can you figure out what the values for weightOnEarth and weightOnMoon are?
        def weightOnMoonResult, weightOnEarthResult
        // ------------ START EDITING HERE ----------------------
        weightOnMoonResult = 1.655
        weightOnEarthResult = 10
        // ------------ STOP EDITING HERE  ----------------------
        assertEquals(weightOnEarth, weightOnEarthResult)
        assertEquals(weightOnMoon, weightOnMoonResult)

        // Now check out this Stackoverflow answer:
        //
        // Create a fake environment using the technique in the link to create a gravity of 6
        // ------------ START EDITING HERE ----------------------
        calculateWeight.delegate = [gravity: 6]
        // ------------ STOP EDITING HERE  ----------------------
        def weightOnFakePlanet = calculateWeight(10)
        assertEquals(60, weightOnFakePlanet)
    }


    void test04_InvokeMethod() {
        // Let's create a Robot that is able to move left, right, up, or down.
        // The Robot should have a (X,Y) coordinates that will change according to the commands it was given.

        Robot robot = new Robot()

        // Add the x,y properties to the Robot to continue...
        assertEquals(0, robot.x)
        assertEquals(0, robot.y)

        // Now add right(), left(), up(), and down(). Change x and y accordingly.
        robot.left()
        robot.left()
        robot.right()
        robot.up()
        robot.down()
        robot.down()

        assertEquals(-1, robot.x)
        assertEquals(-1, robot.y)

        // Wouldn't it be nicer if we could create shorthand versions for combo moves? For example, goLeftLeftRightDown()?
        // Read about invokeMethod() here: http://groovy.codehaus.org/Using+invokeMethod+and+getProperty
        // invokeMethod() allows you to intercept all method calls, even if the method doesn't exist.

        // Using invokeMethod(), handle every possible goXYZ combination... Regular expressions will come in handy.
        robot.goLeftRightRightDown()
        assertEquals([0, -2], [robot.x, robot.y])

        // And what about this option?
        robot.goDownDownDownDown()
        assertEquals([0, -6], [robot.x, robot.y])
    }

    void test05_AddMethodsToExistingObjects() {
        // Finally, we can add methods to existing classes.
        // Have a look at this: http://mrhaki.blogspot.com/2009/12/groovy-goodness-adding-or-overriding.html

        // Using the idea from http://www.codinghorror.com/blog/2007/02/why-cant-programmers-program.html,
        // add a fizzBuzz() method to Integer such that:
        //   - if the integer value is divisible by 3, return 'Fizz'
        //   - if the integer value is divisible by 5, return 'Buzz'
        //   - if it's divisible by both, return 'FizzBuzz'
        //   - otherwise, return the number itself (as a String)

        // ------------ START EDITING HERE ----------------------
        Integer.metaClass.fizzBuzz = {
            String result = ''
            if (delegate % 3 == 0) result += 'Fizz'
            if (delegate % 5 == 0) result += 'Buzz'
            if (!result) result = delegate.toString()
            result
        }
        // ------------ STOP EDITING HERE  ----------------------
        def fizzBuzzes = (1..15).collect { it.fizzBuzz() }
        def correctResult = ['1', '2', 'Fizz', '4', 'Buzz', 'Fizz', '7', '8', 'Fizz',
                'Buzz', '11', 'Fizz', '13', '14', 'FizzBuzz']
        assertEquals(correctResult, fizzBuzzes)
    }

}
