package checks;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OrderController {
    @Transactional
    public String create() { // Noncompliant {{Do not put @Transactional on Controller methods. Move transaction boundary to Service-layer methods.}}
        return "ok";
    }

    public String list() {
        return "ok";
    }
}

@Controller
class PaymentController {
    @Transactional
    public void pay() { // Noncompliant {{Do not put @Transactional on Controller methods. Move transaction boundary to Service-layer methods.}}
        // no-op
    }
}

class OrderService {
    @Transactional
    public void create() {
        // no-op
    }
}
