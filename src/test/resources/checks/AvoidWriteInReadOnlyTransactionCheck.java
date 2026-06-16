package checks;

import org.springframework.transaction.annotation.Transactional;

class AvoidWriteInReadOnlyTransactionCheck {

  interface UserMapper {
    int updateById(String id);
    String selectById(String id);
  }

  private UserMapper userMapper;

  @Transactional(readOnly = true)
  public void noncompliantWrite() {
    userMapper.updateById("1"); // Noncompliant {{Do not perform write operations in @Transactional(readOnly = true) methods.}}
  }

  @Transactional(readOnly = true)
  public String compliantReadOnly() {
    return userMapper.selectById("1");
  }

  @Transactional
  public void writeAllowedInNormalTx() {
    userMapper.updateById("1");
  }
}
