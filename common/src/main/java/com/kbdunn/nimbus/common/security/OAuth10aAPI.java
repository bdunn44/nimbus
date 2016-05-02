package com.kbdunn.nimbus.common.security;

import com.github.scribejava.core.builder.api.DefaultApi10a;

public abstract class OAuth10aAPI implements OAuthAPIService {
	public abstract DefaultApi10a getScribeApi();
}
