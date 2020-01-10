package org.jid.poc.poc2fa.wstrange;

import com.warrenstrange.googleauth.ICredentialRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapCredentialRepository implements ICredentialRepository {

  private Map<String, String> map = new HashMap<>();

  @Override
  public String getSecretKey(String user) {
    return map.get(user);
  }

  @Override
  public void saveUserCredentials(String user, String secret, int verificationCode, List<Integer> scratchCodeList) {
    map.put(user, secret);
  }
}
