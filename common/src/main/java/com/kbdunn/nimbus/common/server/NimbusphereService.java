package com.kbdunn.nimbus.common.server;

import com.kbdunn.nimbus.common.model.nimbusphere.VerifyResponse;

public interface NimbusphereService {
	VerifyResponse verify(String token) throws Exception;
}
