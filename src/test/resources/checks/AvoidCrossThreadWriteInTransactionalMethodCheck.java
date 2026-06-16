package checks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.springframework.transaction.annotation.Transactional;

class AvoidCrossThreadWriteInTransactionalMethodCheck {

  interface OrderMapper {
    int insertOrder(String id);
  }

  private OrderMapper orderMapper;
  private ExecutorService executor;

  @Transactional
  public void noncompliantExecutorSubmit() { // Noncompliant {{Write operations in new threads or async tasks are outside current @Transactional context and may not participate in the transaction.}}
    executor.submit(() -> orderMapper.insertOrder("1"));
  }

  @Transactional
  public void noncompliantCompletableFuture() { // Noncompliant {{Write operations in new threads or async tasks are outside current @Transactional context and may not participate in the transaction.}}
    CompletableFuture.runAsync(() -> orderMapper.insertOrder("2"));
  }

  @Transactional
  public void compliantSameThreadWrite() {
    orderMapper.insertOrder("3");
  }
}
