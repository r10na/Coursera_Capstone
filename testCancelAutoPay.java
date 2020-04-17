package com.main.abp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


import javax.ws.rs.core.Cookie;

import org.apache.commons.chain.Context;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import com.cf.epayments.common.model.TokenResponse;
import com.cf.epayments.common.model.TokenService;
import com.majescomastek.stgicd.billing.dom.CancelAutoPayData;

import com.majescomastek.stgicd.business.components.BaseComponent;
import com.majescomastek.stgicd.constants.IComponentConstants;
import com.majescomastek.stgicd.data.manager.resource.DistributedDataManagerAware;
import com.majescomastek.stgicd.data.manager.resource.IDistributedDataManager;
import com.majescomastek.stgicd.process.engine.authentication.ParameterConfigLoaderComponent;
import com.majescomastek.stgicd.process.engine.business.components.GetModelDataFromCacheComponent;
import com.majescomastek.stgicd.vo.DataHolder;
import com.majescomastek.stgicd.vo.ParameterConfig;
import com.majescomastek.stgicd.vo.Request;
import com.majescomastek.stgicd.vo.Tuple;
import com.majescomastek.stgicd.vo.meta.BaseVO;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class testCancelAutoPay extends BaseComponent implements DistributedDataManagerAware {

	String Token;
	SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-YY");
	Date date = new Date();
	String requestdate = formatter.format(date);

	// @ Value("${tokenservice.url}")
	private String cancelAutopayServiceURL = null, userID = null, password = null,tokenServiceURL=null;

	private IDistributedDataManager dataManager;
	private ParameterConfigLoaderComponent parameterConfigLoaderComponent;
	private GetModelDataFromCacheComponent modelDataCacheComponent;

	public void setDataManager(IDistributedDataManager dataManager) {
		this.dataManager = dataManager;
	}

	String accountNum = null;

	
	String entryDate = null;
	String TransactionId = null;
	String postedBy = null;
	String transaction_status = null;

	String autoPayId = null;

	Date expirationDate1 = null;
	String expirationDate = null;
	

	String postedByType = "SYSTEM";
	String cancellationReasonCd = null;

	public boolean execute(Context context) throws Exception {
		Request requestContext = getInput(context, IComponentConstants.REQUEST_CONTEXT, Request.class);
		String requestID = requestContext.getMetadata().getRequestID().getValue();
		System.out.println("requestID "+requestID);
		Request distributedContext = (Request) dataManager.retrieveDataWithToken(requestID);

		DataHolder dataHolder = distributedContext.getProcessDataHolder();
		Collection<Tuple> tupleCollection = dataHolder.getTupleCollection();

		List<CancelAutoPayData> biList = new ArrayList<CancelAutoPayData>();
		System.out.println("biList "+biList);

		//              testCancelAutoPay autoPay = new testCancelAutoPay();
		//               try {
		//            List<Object> objList = new ArrayList<Object>();
		for (Tuple tuple : tupleCollection) {
			if (tuple.getTupleData() instanceof BaseVO) {
				BaseVO baseVo = (BaseVO) tuple.getTupleData();
				if (baseVo instanceof CancelAutoPayData) {
					biList.add((CancelAutoPayData)baseVo );
				}
			}
			System.out.println("biList 2 "+biList);
			if (biList.size() == 0)
				componentLogger.error("CancelAutoPayData is not loaded in memory...");


			//
			CancelAutoPayData cancelAutoPayData = biList.get(0);
			System.out.println("cancelAutoPayData = biList.get(0): "+cancelAutoPayData);
			try {
				accountNum = cancelAutoPayData.getAccountNum();

//				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//
				Date date = new Date(); 
				entryDate = formatter.format(date);
				System.out.println("entryDate: "+entryDate);
				TransactionId = cancelAutoPayData.getTransactionId();
				postedBy = cancelAutoPayData.getPostedBy();
				transaction_status = cancelAutoPayData.getTransactionStatus();
				autoPayId = cancelAutoPayData.getAutoPayID();

				expirationDate1 = cancelAutoPayData.getExpirationDate();
//				expirationDate1 = cancelAutoPayData.getRequestDate();
				expirationDate = formatter.format(expirationDate1);
				

				postedByType = "SYSTEM";
				cancellationReasonCd = cancelAutoPayData.getCancellationReasonCd();
				System.out.println("cancelAutoPayData :  "+accountNum+" "+entryDate
						+" "+TransactionId+" "+postedBy+" "+expirationDate+" "+cancellationReasonCd);

				System.out.println("start from Server .... \n");
				userID = this.getuserID();
				System.out.println("userID : "+userID);
				password = this.getpassword();
				System.out.println("password : "+password);
				cancelAutopayServiceURL = this.getcancelAutopayServiceURL();
				tokenServiceURL = this.gettokenServiceURL();
				System.out.println("cancelAutopayServiceURL : "+cancelAutopayServiceURL);

				TokenService ts = new TokenService();
				TokenResponse tr = ts.generateNewToken(userID, password, tokenServiceURL);
				System.out.println("value of tr        : " + tr);

				String Token = tr.getSecurityToken();

				System.out.println("value of tr        : " + Token);
				System.out.println("Output from Server .... \n");
				this.cancelAutoPayV1(Token,cancelAutoPayData);

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Exception : "+e);
				cancelAutoPayData.setDescription("SYSTEM ERROR");
				cancelAutoPayData.setProcessStatusFlag("FAILED");

			}

			try {
				dataManager.lockKey(requestID);
				dataManager.storeDataWithToken(requestID, distributedContext);
			} finally {
				dataManager.unlockKey(requestID);
			}
			addOutput(IComponentConstants.REQUEST_CONTEXT, requestContext, context);

		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void cancelAutoPayV1(String Token,CancelAutoPayData cancelAutoPayData ) {
		try {
			this.Token = Token;
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-YY");
			Date date = new Date();
			String requestdate = formatter.format(date);
			System.out.println("request date: " + requestdate);
			Client client = Client.create();

			JSONObject searchRequest = new JSONObject();
			searchRequest.put("entryDate", entryDate+" 00:00:00");
//			searchRequest.put("entryDate", entryDate);
			//searchRequest.put("agentNumber", request.getAgentNumber());
			searchRequest.put("accountNum", accountNum);
			//searchRequest.put("agentNumber", request.getAgentNumber());
			searchRequest.put("postedByType", postedByType);
			searchRequest.put("expirationDate", expirationDate);
			searchRequest.put("postedBy", postedBy);
			//                     searchRequest.put("autoPayId", autoPayId);
			searchRequest.put("cancellationReasonCd", cancellationReasonCd);
			String jsonText = JSONValue.toJSONString(searchRequest);
			System.out.println("jsonText :"+jsonText);

			WebResource webResource = client.resource(cancelAutopayServiceURL);

			ClientResponse response = webResource.type("application/json").cookie(new Cookie("ObSSOCookie", Token))
					.header("TransactionId", TransactionId).header("RequestDate", "2019-08-30 00:00:00")
					.post(ClientResponse.class, jsonText);
			System.out.println("Output response  ....191 \n" +response );
			System.out.println("Output from Server .... \n");
			String output = response.getEntity(String.class);

			System.out.println(output);

			// ----- Data for the pojo class and save in database --Start

			JSONParser parser = new JSONParser();
			//                     CancelAutoPayData cancelAutoPayData =new CancelAutoPayData();
			try {

				Object obj = parser.parse(output);
				JSONObject jsonObject = (JSONObject) obj;
				String transactionId = (String) jsonObject.get("transactionId");
				String transactionStatus = (String) jsonObject.get("transactionStatus");
				System.out.println("transactionId : " + transactionId);
				System.out.println("transactionStatus : " + transactionStatus);
				cancelAutoPayData.setTransactionId("MJCO" + transactionId);
				cancelAutoPayData.setTransactionStatus(transactionStatus);

				String code, description, autoPayId;

				JSONArray messages = (JSONArray) jsonObject.get("messages");
				JSONArray autoPaySetup = (JSONArray) jsonObject.get("autoPaySetup");

				if (autoPaySetup.size() > 0) {
					for (int i = 0; autoPaySetup.size() > i; i++) {
						String autoPaySetup1 = (String) autoPaySetup.get(i).toString();
						Object obj3 = parser.parse(autoPaySetup1);
						JSONObject jsonObject3 = (JSONObject) obj3;
						autoPayId =  (String) jsonObject3.get("autoPayId");
						cancelAutoPayData.setAutoPayID(autoPayId);
						System.out.println("autoPayId : " + autoPayId);

					}
				}

				if (messages.size() > 0) {
					for (int i = 0; messages.size() > i; i++) {
						String message = (String) messages.get(i).toString();
						Object obj2 = parser.parse(message);
						JSONObject jsonObject2 = (JSONObject) obj2;
						String messageType = (String) jsonObject2.get("messageType");
						code = (String) jsonObject2.get("code");
						description = (String) jsonObject2.get("description");
						System.out.println("messages List : " + messageType + "\ncode :  " + code + "\ndescription :  "
								+ description);
						cancelAutoPayData.setCode(code);
						cancelAutoPayData.setDescription(description);
						cancelAutoPayData.setMessageType(messageType);
						System.out.println(" value of transactionStatus " + transactionStatus);
						//                                      
					}
				}

				System.out.println("value of autopay:" + cancelAutoPayData.toString());

				// ----- Data for the pojo class and save in database --END

			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}




	public void setParameterConfigLoaderComponent(ParameterConfigLoaderComponent parameterConfigLoaderComponent) {
		this.parameterConfigLoaderComponent = parameterConfigLoaderComponent;
	}

	public ParameterConfigLoaderComponent getParameterConfigLoaderComponent() {
		return parameterConfigLoaderComponent;
	}

	private String getcancelAutopayServiceURL() throws Exception {
		Map<String, ParameterConfig> map = parameterConfigLoaderComponent.getParamConfigsForCategory("REST_ENDPOINT");
		ParameterConfig paramConfig = (ParameterConfig) map.get("CancelAutoPayClient");
		if (paramConfig == null) {
			return "";
		}
		cancelAutopayServiceURL = paramConfig.getValue();
		return cancelAutopayServiceURL;
	}

	private String getuserID() throws Exception {
		Map<String, ParameterConfig> map = parameterConfigLoaderComponent.getParamConfigsForCategory("REST_CLIENT_SECURITY");
		ParameterConfig paramConfig = (ParameterConfig) map.get("CancelAutoPayClient.oauth2.client_credentials.clientIdentification");
		if (paramConfig == null) {
			return "";
		}
		userID = paramConfig.getValue();
		return userID;
	}

	private String getpassword() throws Exception {
		Map<String, ParameterConfig> map = parameterConfigLoaderComponent.getParamConfigsForCategory("REST_CLIENT_SECURITY");
		ParameterConfig paramConfig = (ParameterConfig) map.get("CancelAutoPayClient.oauth2.client_credentials.clientSecret");
		if (paramConfig == null) {
			return "";
		}
		password = paramConfig.getValue();
		return password;
	}

	private String gettokenServiceURL() throws Exception {
		Map<String, ParameterConfig> map = parameterConfigLoaderComponent.getParamConfigsForCategory("REST_CLIENT_SECURITY");
		ParameterConfig paramConfig = (ParameterConfig) map.get("CancelAutoPayClient.oauth2.client_credentials.accessTokenURI");
		if (paramConfig == null) {
			return "";
		}
		tokenServiceURL = paramConfig.getValue();
		return tokenServiceURL;
	}


}
