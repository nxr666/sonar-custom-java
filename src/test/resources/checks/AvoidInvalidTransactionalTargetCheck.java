package checks;

import org.springframework.transaction.annotation.Transactional;

// ---------- method-level @Transactional on invalid targets ----------

class AvoidInvalidTransactionalTargetCheck {

    @Transactional
    private void privateMethod() { } // Noncompliant {{@Transactional on a private method has no effect; Spring proxy cannot intercept it.}}

    @Transactional
    public static void staticMethod() { } // Noncompliant {{@Transactional on a static method has no effect; Spring proxy cannot intercept it.}}

    @Transactional
    public final void finalMethod() { } // Noncompliant {{@Transactional on a final method has no effect; Spring proxy cannot intercept it.}}

    @Transactional
    public void compliantPublicMethod() { }

    @Transactional
    protected void compliantProtectedMethod() { }
}

// ---------- class-level @Transactional on a final class ----------

@Transactional // Noncompliant {{@Transactional on a final class has no effect; CGLIB cannot subclass it.}}
final class FinalClassWithTransactional {

    public void doSomething() { }
}

// ---------- compliant: non-final class with class-level @Transactional ----------

@Transactional
class NonFinalClassWithTransactional {

    public void doSomething() { }
}
