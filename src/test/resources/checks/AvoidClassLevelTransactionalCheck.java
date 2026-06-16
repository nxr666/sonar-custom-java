package checks;

import org.springframework.transaction.annotation.Transactional;

@Transactional
class NoncompliantClassLevelTransactional { // Noncompliant {{Do not put @Transactional on class declarations. Declare transactions on methods explicitly.}}
    public void process() {
        // no-op
    }
}

class CompliantMethodLevelTransactionalOnly {
    @Transactional
    public void process() {
        // no-op
    }
}

class CompliantWithoutClassLevelTransactional {
    public void process() {
        // no-op
    }
}
