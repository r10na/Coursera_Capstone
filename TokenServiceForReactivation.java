package com.cf.epayments.common.model;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
/*import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;*/
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
/*import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;*/
import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.cf.epayments.common.model.TokenResponse;
import com.cf.epayments.common.model.TokenServiceInput;
//import com.fasterxml.jackson.annotation.JsonInclude;
import com.majescomastek.stgicd.vo.ParameterConfig;
import com.majescomastek.stgicd.data.manager.resource.DistributedDataManagerAware;
import com.majescomastek.stgicd.data.manager.resource.IDistributedDataManager;
import com.majescomastek.stgicd.process.engine.authentication.ParameterConfigLoaderComponent;
import com.majescomastek.stgicd.process.engine.business.components.GetModelDataFromCacheComponent;

@Service
//@RefreshScope
public class TokenServiceForReactivation implements DistributedDataManagerAware {
	
	

	private final Logger logger = LoggerFactory.getLogger(TokenServiceForReactivation.class);
	@Autowired
	@Qualifier("restTemplate")
	private RestTemplate restTemplate = new RestTemplate();
	
	//add for certification
	 

	private ParameterConfigLoaderComponent parameterConfigLoaderComponent;

	//	@ Value("${tokenservice.url}")
	 String tokenServiceURL = "https://stsqa.countrypassport.net/STS/GetSecurityToken"; //null

	private static Map<String, TokenResponse> TOKEN_RESPONSE = new ConcurrentHashMap<>();

	/*	public void setTokenServiceURL(String tokenServiceURL) {
		this.tokenServiceURL = tokenServiceURL;
	}
	 */
	public TokenResponse generateToken(String userId, String password, boolean refreshToken) {
		/*
		 * generate new token if not available in Map. Generate new token if caller is
		 * asking for new token and retries aren't exhausted. Generate new token if
		 * retries aren't exhausted
		 */
		
		 
		// password = "123";
		if (!TOKEN_RESPONSE.containsKey(userId)
				|| (refreshToken && TOKEN_RESPONSE.containsKey(userId)
						&& TOKEN_RESPONSE.get(userId).getSecurityToken() != null)
				|| (refreshToken && TOKEN_RESPONSE.containsKey(userId)
						&& TOKEN_RESPONSE.get(userId).getSecurityToken() == null
						&& TOKEN_RESPONSE.get(userId).getRetryCount().get() < 3)
				|| (TOKEN_RESPONSE.containsKey(userId) && TOKEN_RESPONSE.get(userId).getSecurityToken() == null
				&& TOKEN_RESPONSE.get(userId).getRetryCount().get() < 3)) 
		{
			System.out.println("contains key");
			return generateNewToken(userId, password);
		} 
		else if (TOKEN_RESPONSE.containsKey(userId) && TOKEN_RESPONSE.get(userId).getSecurityToken() == null
				&& TOKEN_RESPONSE.get(userId).getRetryCount().get() >= 3) {
			logger.error(
					"Unable to fetch token for user id " + userId + " , account is locked or password has changed.");
			return null;
		}
		TokenResponse token = TOKEN_RESPONSE.get(userId);
		System.out.println(token);
		return TOKEN_RESPONSE.get(userId);
	}

	public TokenResponse generateNewToken(String userId, String password) {
		TokenResponse resp = null;
		restTemplate = new RestTemplate();

		System.out.println("line number 100 we are in");
		synchronized (this) {
			if (TOKEN_RESPONSE.containsKey(userId) && TOKEN_RESPONSE.get(userId).getLastUpdate() != null) {
				System.out.println("line number 103 we are in");
				long seconds = TOKEN_RESPONSE.get(userId).getLastUpdate().until(LocalDateTime.now(),
						ChronoUnit.SECONDS);
				if (seconds < 10) {
					System.out.println("line number 107 we are in");// check if token updated recently, then return same token
					return TOKEN_RESPONSE.get(userId);
				}

			}
			
			//System.out.println("line number 102 we are in");
			TokenServiceInput tokenServiceInput = new TokenServiceInput(userId, password);
			//System.out.println("line number 104 we are in"+userId+password);
			
			try {
				System.out.println("line number 118 we are in "+userId+password);
				HttpEntity<TokenServiceInput> httpEntity = new HttpEntity<TokenServiceInput>(tokenServiceInput);
				//System.out.println("line number httpEntity we are in : "+ httpEntity);

//				tokenServiceURL = gettokenServiceURL();
				tokenServiceURL = "https://stsqa.countrypassport.net/STS/GetSecurityToken";
			 System.out.println("this is tokenservice url " +tokenServiceURL);
			 
			 
			 
			 
				
				resp = (TokenResponse)restTemplate.postForObject(tokenServiceURL, httpEntity, TokenResponse.class);

				System.out.println("response.getBody() :"+resp);
				resp = restTemplate.postForObject(tokenServiceURL, httpEntity, TokenResponse.class);
				if (resp != null) {
					System.out.println("line number 82 we are in");
					 //resp.setSecurityToken("1234");
					resp.setLastUpdate(LocalDateTime.now());
					resp.getRetryCount().set(0);
					TOKEN_RESPONSE.put(userId, resp);
					return resp;
				} else  {
					logger.error("Unable to fetch token for user id " + userId
							+ " , account is locked or password has changed.");
					System.out.println("response is NULL line 144");
					if (TOKEN_RESPONSE.containsKey(userId)) {
						resp = TOKEN_RESPONSE.get(userId);
					} else {
						resp = new TokenResponse();
						TOKEN_RESPONSE.put(userId, resp);
					}
					resp.getRetryCount().incrementAndGet();
					return null;
				}

			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage(), e);
			}
		}
		return resp;
	}

	private String gettokenServiceURL() throws Exception {
		System.out.println("i am in gettokenserviceurl line 155");
		Map<String, ParameterConfig> map = parameterConfigLoaderComponent.getParamConfigsForCategory("REST_CLIENT_SECURITY");
		ParameterConfig paramConfig = (ParameterConfig) map.get("ActiveTermUpdateClient.oauth2.client_credentials.accessTokenURI");
		System.out.println("this is paramconfig (line 157) : " +paramConfig);
		if (paramConfig == null) {
			
			return "";
		}
		tokenServiceURL = paramConfig.getValue();
		System.out.println("i am here");
		return tokenServiceURL;
	}

	public void setParameterConfigLoaderComponent(ParameterConfigLoaderComponent parameterConfigLoaderComponent) {
		this.parameterConfigLoaderComponent = parameterConfigLoaderComponent;
	}

	public ParameterConfigLoaderComponent getParameterConfigLoaderComponent() {
		return parameterConfigLoaderComponent;
	}

	@Override
	public void setDataManager(IDistributedDataManager arg0) {
		// TODO Auto-generated method stub

	}


}
