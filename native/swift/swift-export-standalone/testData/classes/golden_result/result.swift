import KotlinBridges
import KotlinRuntime

/**
* this is a sample comment for class without public constructor
*/
public class ClassWithNonPublicConstructor {
    public var a: Swift.Int32 {
        get {
            fatalError()
        }
    }
}
/**
* this is a sample comment for class without package
* in order to support documentation for primary constructor - we will have to start parsing comment content:
* https://kotlinlang.org/docs/kotlin-doc.html#constructor
*/
public class Foo {
    /**
    * this is a sample comment for INSIDE_CLASS without package
    */
    public class INSIDE_CLASS {
        /**
        * this is a sample comment for val on INSIDE_CLASS without package
        */
        public var my_value_inner: Swift.UInt32 {
            get {
                fatalError()
            }
        }
        /**
        * this is a sample comment for var on INSIDE_CLASS without package
        */
        public var my_variable_inner: Swift.Int64 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        /**
        * this is a sample comment for func on INSIDE_CLASS without package
        */
        public func my_func() -> Swift.Bool {
            fatalError()
        }
        /**
        * this is a sample comment for INSIDE_CLASS without package
        */
        public init() {
            fatalError()
        }
    }
    /**
    * this is a sample comment for val on class without package
    */
    public var my_value: Swift.UInt32 {
        get {
            fatalError()
        }
    }
    /**
    * this is a sample comment for var on class without package
    */
    public var my_variable: Swift.Int64 {
        get {
            fatalError()
        }
        set {
            fatalError()
        }
    }
    /**
    * this is a sample comment for func on class without package
    */
    public func foo() -> Swift.Bool {
        fatalError()
    }
    public init(
        a: Swift.Int32
    ) {
        fatalError()
    }
    /**
    * this is a sample comment for secondary constructor
    */
    public init(
        f: Swift.Float
    ) {
        fatalError()
    }
}
/**
demo comment for packageless object
*/
public class OBJECT_NO_PACKAGE {
    public class Foo {
        public init() {
            fatalError()
        }
    }
    public class Bar {
        public class CLASS_INSIDE_CLASS_INSIDE_OBJECT {
            public init() {
                fatalError()
            }
        }
        public var i: Swift.Int32 {
            get {
                fatalError()
            }
        }
        public func bar() -> Swift.Int32 {
            fatalError()
        }
        public init(
            i: Swift.Int32
        ) {
            fatalError()
        }
    }
    public static var shared: Swift.Int32 {
        get {
            fatalError()
        }
    }
    public var value: Swift.Int32 {
        get {
            fatalError()
        }
    }
    public var variable: Swift.Int32 {
        get {
            fatalError()
        }
        set {
            fatalError()
        }
    }
    private init() {
        fatalError()
    }
    public func foo() -> Swift.Int32 {
        fatalError()
    }
}
public extension main.namespace.deeper {
    public class NAMESPACED_CLASS {
        public init() {
            fatalError()
        }
    }
    public class Foo {
        public class INSIDE_CLASS {
            public class DEEPER_INSIDE_CLASS {
                public var my_value: Swift.UInt32 {
                    get {
                        fatalError()
                    }
                }
                public var my_variable: Swift.Int64 {
                    get {
                        fatalError()
                    }
                    set {
                        fatalError()
                    }
                }
                public func foo() -> Swift.Bool {
                    fatalError()
                }
                public init() {
                    fatalError()
                }
            }
            public var my_value: Swift.UInt32 {
                get {
                    fatalError()
                }
            }
            public var my_variable: Swift.Int64 {
                get {
                    fatalError()
                }
                set {
                    fatalError()
                }
            }
            public func foo() -> Swift.Bool {
                fatalError()
            }
            public init() {
                fatalError()
            }
        }
        public var my_value: Swift.UInt32 {
            get {
                fatalError()
            }
        }
        public var my_variable: Swift.Int64 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        public func foo() -> Swift.Bool {
            fatalError()
        }
        public init() {
            fatalError()
        }
    }
    /**
    demo comment for packaged object
    */
    public class OBJECT_WITH_PACKAGE {
        public class Foo {
            public init() {
                fatalError()
            }
        }
        public class Bar {
            /**
            * demo comment for inner object
            */
            public class OBJECT_INSIDE_CLASS {
                public static var shared: Swift.Int32 {
                    get {
                        fatalError()
                    }
                }
                private init() {
                    fatalError()
                }
            }
            public var i: Swift.Int32 {
                get {
                    fatalError()
                }
            }
            public func bar() -> Swift.Int32 {
                fatalError()
            }
            public init(
                i: Swift.Int32
            ) {
                fatalError()
            }
        }
        public static var shared: Swift.Int32 {
            get {
                fatalError()
            }
        }
        public var value: Swift.Int32 {
            get {
                fatalError()
            }
        }
        public var variable: Swift.Int32 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        private init() {
            fatalError()
        }
        public func foo() -> Swift.Int32 {
            fatalError()
        }
    }
}
public extension main.namespace {
    /**
    *  demo comment for
    *  NAMESPACED_CLASS
    */
    public class NAMESPACED_CLASS {
        /**
        *  demo comment for
        *  NAMESPACED_CLASS
        */
        public init() {
            fatalError()
        }
    }
    public class Foo {
        /**
        * this is a sample comment for INSIDE_CLASS with package
        */
        public class INSIDE_CLASS {
            /**
            * this is a sample comment for INSIDE_CLASS with package
            */
            public init() {
                fatalError()
            }
        }
        /**
        * this is a sample comment for val on class with package
        */
        public var my_value: Swift.UInt32 {
            get {
                fatalError()
            }
        }
        /**
        * this is a sample comment for var on class with package
        */
        public var my_variable: Swift.Int64 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        /**
        * this is a sample comment for func on class with package
        */
        public func foo() -> Swift.Bool {
            fatalError()
        }
        public init() {
            fatalError()
        }
    }
}
public enum namespace {
    public enum deeper {
    }
}
