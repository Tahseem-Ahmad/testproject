package com.mli.productrate.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mli.productrate.request.PlanCodes;
import com.mli.productrate.request.ProductRateCalculatorApiRequest;
import com.mli.productrate.request.RequestPlan;
import com.mli.productrate.request.RequestRider;
import com.mli.productrate.response.PlanDetailBean;
import com.mli.productrate.response.ProductRateCalculatorApiResponse;
import com.mli.productrate.response.ProductRateCalculatorResponse;
import com.mli.productrate.response.ResponsePayload;
import com.mli.productrate.response.ResponsePlan;
import com.mli.productrate.response.ResponseRider;
import com.mli.productrate.service.PlanDetail;
import com.mli.productrate.service.ProductRateCalulator;
import com.mli.productrate.util.Header;
import com.mli.productrate.util.MessageConstants;
import com.mli.productrate.util.MessageInfo;


@Service
public class ProductRateCalulatorService implements ProductRateCalulator{

	Logger logger =LoggerFactory.getLogger(ProductRateCalulatorService.class);

	@Autowired
	PlanCodes planCodes;

	@Autowired
	PlanDetail planDetail;

	@Autowired
	Environment environment;

	@Override
	public ProductRateCalculatorApiResponse calculateProductRate(ProductRateCalculatorApiRequest productRateCalculatorApiRequest) {
		double riderPremium =0.0;
		double riderWGST =0.0;
		double totalvalueAdd =0.0;
		double totalvaluePlanAdd =0.0;
		double riderrate=0.0;
		double riderMode=0;
		double planBaseAnnualPremium;
		double planBasePremium;
		double gst;
		double planWGST;
		double tccpabBaseAnnualPremium;
		double gstRider;
		double wopSumAssured;
		double tccibBaseAnnualPremium;
		double totalPremiumWOGST;
		double totalPremiumWGST;
		double planWGSTTotal;
		double modeValue=0;
		double totalRiderPremium;
		double totalRiderWGST;
		double rate;
		double totalriderWoGST = 0;
		StringBuilder planId =new StringBuilder();
		String riderIdDCCCP ="";
		String riderIdVN05 ="";

		ResponseRider resRider =null;
		Header header=null;		
		ResponsePlan resPlan =null;
		MessageInfo msginfo = new MessageInfo();
		ProductRateCalculatorResponse response = new ProductRateCalculatorResponse();
		ProductRateCalculatorApiResponse apiResponse =new ProductRateCalculatorApiResponse();
		ResponsePayload payloadResGetDemoAuthRequest = new ResponsePayload();

		List<Map<String , String>> resultPlanDetailProc =null;
		List<PlanDetailBean> planDetailBeanList =null;
		List<ResponsePlan> listResPlan = new ArrayList<>();
		Map<String,Double> modelPremiumForVN05 = new HashMap<>();
		try{
			if (validateHeader(productRateCalculatorApiRequest) ){
				header=productRateCalculatorApiRequest.getRequest().getHeader();
				for(RequestPlan reqPlan:productRateCalculatorApiRequest.getRequest().getPayload().getReqPlan()){
					modeValue=0;
					if(planCodes.getPlanIdforDB().contains(reqPlan.getPlanId())){
						if(reqPlan.getPlanId()!=null && planCodes.getPlancodelistTCOT60TNOT60().contains(reqPlan.getPlanId().trim())){
							planId.append(planCodes.getPlanTcot60());
						}
						else if(reqPlan.getPlanId()!=null && planCodes.getPlancodelistTCOTP2TNOTP2().contains(reqPlan.getPlanId().trim())){
							planId.append(MessageConstants.PLANID_TCOTP2);
						}

						if(reqPlan.getReqRider() !=null && !reqPlan.getReqRider().isEmpty()){
							for(int i=0 ;i<reqPlan.getReqRider().size();i++){
								if(planCodes.getPlanIdforDB().contains(reqPlan.getReqRider().get(i).getRiderId())){
									if(reqPlan.getReqRider().get(i).getRiderId()!=null && planCodes.getPlancodelistTCCIBTNCIB().contains(reqPlan.getReqRider().get(i).getRiderId().trim())){
										planId.append(",").append(MessageConstants.PLANID_TCCIB);
									}
								}
								else if(planCodes.getRiderIdforDB().contains(reqPlan.getReqRider().get(i).getRiderId())){
									if(reqPlan.getReqRider().get(i).getRiderId()!=null && planCodes.getPlancodelistDCCCP().contains(reqPlan.getReqRider().get(i).getRiderId().trim())){
										riderIdDCCCP =MessageConstants.PLANID_DCCCP;
									}
									else if(reqPlan.getReqRider().get(i).getRiderId() !=null && planCodes.getPlancodelistVN05VN04().contains(reqPlan.getReqRider().get(i).getRiderId().trim())){
										riderIdVN05=MessageConstants.PLANID_VN05;
									}

								}
							}
						}
					}
					resultPlanDetailProc = planDetail.getPlancDetails(planId.toString(),reqPlan.getAge()!=null?reqPlan.getAge().trim():"",reqPlan.getGender()!=null?reqPlan.getGender():"",("Y").equalsIgnoreCase(reqPlan.getEmpDiscount())?"Y":"No","premiumCalc");
					if(!("").equalsIgnoreCase(riderIdDCCCP)){
						planDetailBeanList=planDetail.callPlanDetailService(riderIdDCCCP,reqPlan.getAge()!=null?reqPlan.getAge().trim():"",reqPlan.getGender()!=null?reqPlan.getGender():"");
						logger.debug(""+planDetailBeanList);
					}
					if(!("").equalsIgnoreCase(riderIdVN05)){
						planDetailBeanList=planDetail.callPlanDetailService(riderIdVN05,reqPlan.getAge()!=null?reqPlan.getAge().trim():"",reqPlan.getGender()!=null?reqPlan.getGender():"");
						logger.debug(""+planDetailBeanList);
					}
					if((resultPlanDetailProc!=null && !resultPlanDetailProc.isEmpty()) ){
						if(!("").equalsIgnoreCase(riderIdVN05) && planDetailBeanList.isEmpty()
								|| (!("").equalsIgnoreCase(riderIdDCCCP) && planDetailBeanList.isEmpty())){
							msginfo.setMsgCode(MessageConstants.C700);
							msginfo.setMsg(MessageConstants.FAILURE);
							msginfo.setMsgDescription(MessageConstants.C700DESC);
							break;
						}
						totalRiderPremium = 0.0;
						totalRiderWGST =0.0;
						List<ResponseRider> listResRider = new ArrayList<>();
						resPlan =new ResponsePlan();
						resPlan.setPlanId(reqPlan.getPlanId());
						resPlan.setVariantId(reqPlan.getVariantId());
						resPlan.setGender(reqPlan.getGender());
						resPlan.setAge(reqPlan.getAge());
						resPlan.setEmpDiscount(reqPlan.getEmpDiscount());
						resPlan.setPlanSumAssured(reqPlan.getPlanSumAssured());
						resPlan.setPolicyTerm(reqPlan.getPolicyTerm());
						resPlan.setPolicyPayTerm(reqPlan.getPolicyPayTerm());
						resPlan.setSmoke(reqPlan.getSmoke());
						resPlan.setMode(reqPlan.getMode());

						// for rate we check annual premium and policyTerm and varientId and smoker and non smoker and take rate for varient
						rate=0.0;
						if(reqPlan.getPlanId()!=null && !("").equalsIgnoreCase(reqPlan.getPlanId())){
							String ammount="";
							long premiumAmount=reqPlan.getPlanSumAssured()!=null?Long.valueOf(reqPlan.getPlanSumAssured().trim()):0;

							if(premiumAmount >= 0 && premiumAmount <= Long.parseLong(environment.getProperty("productrate.premiumamount.range1"))){										  
								ammount=environment.getProperty("productrate.premiumamount.range1");
							}
							else if(premiumAmount >= Long.parseLong(environment.getProperty("productrate.premiumamount.range2")) && premiumAmount <= Long.parseLong(environment.getProperty("productrate.premiumamount.range3"))){
								ammount=environment.getProperty("productrate.premiumamount.range3");
							}
							else if(premiumAmount >= Long.parseLong(environment.getProperty("productrate.premiumamount.range4")) && premiumAmount <= Long.parseLong(environment.getProperty("productrate.premiumamount.range5"))){
								ammount=environment.getProperty("productrate.premiumamount.range5");
							}
							else if(premiumAmount >= Long.parseLong(environment.getProperty("productrate.premiumamount.range6")) && premiumAmount <= Long.parseLong(environment.getProperty("productrate.premiumamount.range7"))){
								ammount=environment.getProperty("productrate.premiumamount.range7");
							}
							logger.debug("Premium amount for search in database values ::" +ammount);
							if(reqPlan.getPlanId() !=null && planCodes.getPlancodelistTCOTP2TNOTP2().contains(reqPlan.getPlanId().trim())){
								logger.debug("plan Id for TCOTP2 start");
								rate = rateCountForTCOTP2(resultPlanDetailProc,reqPlan,ammount);
								logger.debug("rate value is :: " +rate);
								modeValue=modeCount(reqPlan);
								logger.debug("modeValue  is :: " +modeValue);
							}
							else if(reqPlan.getPlanId() !=null && planCodes.getPlancodelistTCOT60TNOT60().contains(reqPlan.getPlanId().trim())){
								logger.debug("plan Id for TCOT60 start");
								rate = rateCountForTCOT60(resultPlanDetailProc,reqPlan,ammount);
								logger.debug("rate value is :: " +rate);
								modeValue=modeCount(reqPlan);
								logger.debug("modeValue  is :: " +modeValue);
							}
						}
						//multiply by one because it take annual premium
						planBaseAnnualPremium = (Double.valueOf(reqPlan.getPlanSumAssured().trim()) * rate * 1 )/1000;
						modelPremiumForVN05.put("PLANPremium", planBaseAnnualPremium);
						planBasePremium =(modeCountValue((Double.valueOf(reqPlan.getPlanSumAssured().trim()) * rate * modeValue )/1000,reqPlan.getMode()));
						logger.debug("planBasePremium is ::" +planBasePremium);
						BigDecimal valuePlanPremiumTCCPAB= BigDecimal.valueOf(planBasePremium);
						valuePlanPremiumTCCPAB = valuePlanPremiumTCCPAB.setScale(2, BigDecimal.ROUND_HALF_UP);
						resPlan.setPlanBasePremium(valuePlanPremiumTCCPAB.toString());
						gst = ((planBasePremium * 18)/100) ;
						logger.debug("plan GST is ::" +gst);
						BigDecimal valuePlanGSTTCCPAB= BigDecimal.valueOf(gst);
						valuePlanGSTTCCPAB = valuePlanGSTTCCPAB.setScale(2, BigDecimal.ROUND_HALF_UP);
						BigDecimal valuePlan= BigDecimal.valueOf(planBasePremium + gst);
						totalvaluePlanAdd =valuePlan.doubleValue();
						valuePlan = valuePlan.setScale(0, BigDecimal.ROUND_HALF_UP);
						planWGST=valuePlan.doubleValue();
						logger.debug("planWGST is ::" +planWGST);
						resPlan.setPlanWGST(Double.toString(planWGST));
						resPlan.setPlanGST(valuePlanGSTTCCPAB.toString());
						if(reqPlan.getReqRider() != null && !reqPlan.getReqRider().isEmpty()){
							for(RequestRider reqRider : reqPlan.getReqRider()){
								riderrate=0.0;
								riderMode=0;
								if(reqRider.getRiderId()!=null && planCodes.getPlancodelistTCCIBTNCIB().contains(reqRider.getRiderId().trim())){
									logger.debug("Plan Id for TCCIB start");
									riderrate=rateCountForRiderTCCIB(resultPlanDetailProc,reqRider);
									logger.debug("Rider rate ::" +riderrate);
									riderMode=modeCountRider(reqPlan,reqRider.getRiderId().trim());
									logger.debug("RiderMode rate ::" +riderMode);
								}

								else if(reqRider.getRiderId()!=null && planCodes.getPlancodelistDCCCP().contains(reqRider.getRiderId().trim())){
									logger.debug("plan Id for DCCCP start");
									String discount=("Y").equalsIgnoreCase(reqPlan.getEmpDiscount())?"ED":"";
									riderrate=rateCountForRiderDCCCP(planDetailBeanList,reqRider,discount);
									logger.debug("Rider rate ::" +riderrate);
									riderMode=modeCountRider(reqPlan,reqRider.getRiderId().trim());
									logger.debug("RiderMode rate ::" +riderMode);
								}
								else if(reqRider.getRiderId()!=null && planCodes.getPlancodelistTCCPABTNCPAB().contains(reqRider.getRiderId().trim())){
									logger.debug("Plan Id for TCCPAB start");
									resRider = new ResponseRider();
									riderMode=modeCountRider(reqPlan,reqRider.getRiderId().trim());
									logger.debug("riderMode rate ::" +riderMode);
									resRider.setRiderId(reqRider.getRiderId().trim());
									resRider.setRiderSA(reqRider.getRiderSA().trim());
									resRider.setRiderTerm(reqRider.getRiderTerm().trim());
									//multiply by one because it take annual premium
									tccpabBaseAnnualPremium = (Double.valueOf(reqRider.getRiderSA().trim()) * 0.63 * 1)/1000;
									modelPremiumForVN05.put(reqRider.getRiderId().trim(), tccpabBaseAnnualPremium);
									riderPremium =(modeCountValue((Double.valueOf(reqRider.getRiderSA().trim()) * 0.63 * riderMode)/1000,reqPlan.getMode()));
									logger.debug("RiderPremium is ::" +riderPremium);
									BigDecimal valueriderPremiumTCCPAB= BigDecimal.valueOf(riderPremium);
									valueriderPremiumTCCPAB = valueriderPremiumTCCPAB.setScale(2, BigDecimal.ROUND_HALF_UP);
									totalriderWoGST=valueriderPremiumTCCPAB.doubleValue();
									resRider.setRiderPremium(valueriderPremiumTCCPAB.toString());
									gstRider = ((riderPremium * 18)/100 );
									logger.debug("RiderWGST GST is ::" +gstRider);
									BigDecimal valueriderGSTTCCPAB= BigDecimal.valueOf(gstRider);
									valueriderGSTTCCPAB = valueriderGSTTCCPAB.setScale(2, BigDecimal.ROUND_HALF_UP);
									resRider.setRiderGST(valueriderGSTTCCPAB.toString());
									BigDecimal valuerider= BigDecimal.valueOf(riderPremium + gstRider);
									totalvalueAdd = valuerider.doubleValue();
									valuerider = valuerider.setScale(0, BigDecimal.ROUND_HALF_UP);
									riderWGST = valuerider.doubleValue();
									logger.debug("RiderWGST is ::" +riderWGST);
									resRider.setRiderWGST(Double.toString(riderWGST));
								}
								else if(reqRider.getRiderId()!=null && planCodes.getPlancodelistVN05VN04().contains(reqRider.getRiderId().trim())){
									logger.info("Plan Id for VN05 start");
									resRider = new ResponseRider();
									resRider.setRiderId(reqRider.getRiderId().trim());
									resRider.setRiderTerm(reqRider.getRiderTerm().trim());
									riderMode=modeCountRider(reqPlan,reqRider.getRiderId().trim());
									logger.debug("RiderMode rate ::" +riderMode);

									double wopsumdata =0.0;
									for (Map.Entry<String,Double> entry : modelPremiumForVN05.entrySet()) {	
										wopsumdata +=entry.getValue();
									}
									wopSumAssured =wopsumdata*riderMode;
									wopSumAssured = wopSumAssured >= 350000 ? 350000 :wopSumAssured;
									BigDecimal valueridersumAssured= BigDecimal.valueOf(wopSumAssured);
									valueridersumAssured = valueridersumAssured.setScale(2, BigDecimal.ROUND_HALF_UP);
									resRider.setRiderSA(valueridersumAssured.toString());
									riderrate=rateCountForRiderVN05(planDetailBeanList,reqRider,wopSumAssured);
									logger.debug("Rider rate ::" +riderrate);
									riderPremium =(modeCountValue((wopSumAssured * riderrate )/1000,reqPlan.getMode()));
									logger.debug("RiderPremium is ::" +riderPremium);
									BigDecimal valueriderPremiumTCCPAB= BigDecimal.valueOf(riderPremium);
									valueriderPremiumTCCPAB = valueriderPremiumTCCPAB.setScale(2, BigDecimal.ROUND_HALF_UP);
									totalriderWoGST=valueriderPremiumTCCPAB.doubleValue();
									resRider.setRiderPremium(valueriderPremiumTCCPAB.toString());
									gstRider = ((riderPremium * 18)/100 );
									logger.debug("RiderWGST GST is ::" +gstRider);
									BigDecimal valueriderGSTTCCPAB= BigDecimal.valueOf(gstRider);
									valueriderGSTTCCPAB = valueriderGSTTCCPAB.setScale(2, BigDecimal.ROUND_HALF_UP);
									resRider.setRiderGST(valueriderGSTTCCPAB.toString());
									BigDecimal valuerider=BigDecimal.valueOf(riderPremium + gstRider);
									totalvalueAdd = valuerider.doubleValue();
									valuerider = valuerider.setScale(0, BigDecimal.ROUND_HALF_UP);
									riderWGST = valuerider.doubleValue();
									logger.debug("RiderWGST is ::" +riderWGST);
									resRider.setRiderWGST(Double.toString(riderWGST));
								}

								if(reqRider!=null && (planCodes.getPlancodelistTCCIBTNCIB().contains(reqRider.getRiderId().trim()) 
										|| planCodes.getPlancodelistDCCCP().contains(reqRider.getRiderId().trim()))){
									resRider = new ResponseRider();
									resRider.setRiderId(reqRider.getRiderId().trim());
									resRider.setRiderSA(reqRider.getRiderSA().trim());
									resRider.setRiderTerm(reqRider.getRiderTerm().trim());

									if(planCodes.getPlancodelistTCCIBTNCIB().contains(reqRider.getRiderId().trim())){
										//multiply by one because it take annual premium
										tccibBaseAnnualPremium = (Double.valueOf(reqRider.getRiderSA().trim()) * riderrate * 1)/1000;
										modelPremiumForVN05.put(reqRider.getRiderId(), tccibBaseAnnualPremium);
										riderPremium =(modeCountValue((Double.valueOf(reqRider.getRiderSA()!=""?reqRider.getRiderSA():"0.0") * riderrate * riderMode)/1000,reqPlan.getMode()));
									}
									if(planCodes.getPlancodelistDCCCP().contains(reqRider.getRiderId().trim())){
										riderPremium =((Double.valueOf(reqRider.getRiderSA()!=""?reqRider.getRiderSA():"0.0") * riderrate * riderMode)/1000);
									}
									BigDecimal valueriderPremium=BigDecimal.valueOf(riderPremium);
									valueriderPremium = valueriderPremium.setScale(2, BigDecimal.ROUND_HALF_UP);
									logger.debug("RiderPremium is ::" +riderPremium);
									totalriderWoGST=valueriderPremium.doubleValue();
									resRider.setRiderPremium((valueriderPremium.toString()));
									gstRider = ((riderPremium * 18)/100 );
									logger.debug("RiderWGST GST is ::" +gstRider);
									BigDecimal valueriderGST=BigDecimal.valueOf(gstRider);
									valueriderGST = valueriderGST.setScale(2, BigDecimal.ROUND_HALF_UP);
									resRider.setRiderGST(valueriderGST.toString());
									BigDecimal valueriderOther= BigDecimal.valueOf(riderPremium + gstRider);
									totalvalueAdd = valueriderOther.doubleValue();
									valueriderOther = valueriderOther.setScale(0, BigDecimal.ROUND_HALF_UP);
									riderWGST=valueriderOther.doubleValue();
									logger.debug("RiderWGST is ::" +riderWGST);
									resRider.setRiderWGST(Double.toString(riderWGST));
								}
								if(resRider.getRiderId().equalsIgnoreCase(reqRider.getRiderId())){
									listResRider.add(resRider);
									totalRiderPremium += totalriderWoGST;
									totalRiderWGST +=totalvalueAdd;
								}
								else{
									resRider = new ResponseRider();
									resRider.setRiderId(reqRider.getRiderId().trim());
									resRider.setRiderSA(reqRider.getRiderSA().trim());
									resRider.setRiderTerm(reqRider.getRiderTerm().trim());
									listResRider.add(resRider);
								}
							}
						}
						BigDecimal totalRiderPremium2Scale= BigDecimal.valueOf(totalRiderPremium);
						totalRiderPremium2Scale = totalRiderPremium2Scale.setScale(2, BigDecimal.ROUND_HALF_UP);
						resPlan.setTotalRiderPremiumWOGST((totalRiderPremium2Scale).toString());
						logger.debug("totalRiderPremiumWOGST is ::" +totalRiderPremium);
						BigDecimal totalRiderWGST2Scale= BigDecimal.valueOf(totalRiderWGST);
						totalRiderWGST2Scale = totalRiderWGST2Scale.setScale(2, BigDecimal.ROUND_HALF_UP);
						resPlan.setTotalRiderPremiumWGST((totalRiderWGST2Scale).toString());
						logger.debug("setTotalRiderPremiumWGST is ::" +totalRiderWGST);
						totalPremiumWOGST =totalRiderPremium+planBasePremium;
						logger.debug("totalPremiumWOGST is ::" +totalPremiumWOGST);
						totalPremiumWGST =totalRiderWGST+totalvaluePlanAdd;
						logger.debug("totalPremiumWGST is ::" +totalPremiumWGST);
						BigDecimal totalPremiumWGST2Scale=BigDecimal.valueOf(totalPremiumWGST);
						totalPremiumWGST2Scale = totalPremiumWGST2Scale.setScale(0, BigDecimal.ROUND_HALF_UP);
						planWGSTTotal=totalPremiumWGST2Scale.doubleValue();
						resPlan.setTotalPremiumWGST(Double.toString(planWGSTTotal));
						logger.debug("setTotalPremiumWGST is ::" +totalPremiumWGST);
						BigDecimal totalPremiumWOGST2Scale=BigDecimal.valueOf(totalPremiumWOGST);
						totalPremiumWOGST2Scale = totalPremiumWOGST2Scale.setScale(2, BigDecimal.ROUND_HALF_UP);
						resPlan.setTotalPremiumWOGST((totalPremiumWOGST2Scale).toString());
						logger.debug("setTotalPremiumWOGST is ::" +totalPremiumWOGST);
						resPlan.setResRider(listResRider);
						listResPlan.add(resPlan);
					}
					else{
						msginfo.setMsgCode(MessageConstants.C700);
						msginfo.setMsg(MessageConstants.FAILURE);
						msginfo.setMsgDescription(MessageConstants.C700DESC);
						logger.info(MessageConstants.C700DESC);
					}
				}
				if(listResPlan != null && !listResPlan.isEmpty()){
					payloadResGetDemoAuthRequest.setResPlan(listResPlan);
					response.setPayload(payloadResGetDemoAuthRequest);
					msginfo.setMsgCode(MessageConstants.C200);
					msginfo.setMsg(MessageConstants.SUCCESS);
					msginfo.setMsgDescription(MessageConstants.C200DESC);
				}
				else if(msginfo.getMsgCode() == null || ("").equalsIgnoreCase(msginfo.getMsgCode())){
					msginfo.setMsgCode(MessageConstants.C500);
					msginfo.setMsg(MessageConstants.FAILURE);
					msginfo.setMsgDescription(MessageConstants.C605DESC);
				}
			}
			else{
				msginfo.setMsgCode(MessageConstants.C600);
				msginfo.setMsg(MessageConstants.FAILURE);
				msginfo.setMsgDescription(MessageConstants.C600DESC);
				apiResponse = new ProductRateCalculatorApiResponse(new ProductRateCalculatorResponse(header, msginfo, null));
			}


		} 
		catch (Exception e){
			msginfo.setMsgCode(MessageConstants.C500);
			msginfo.setMsg(MessageConstants.FAILURE);
			msginfo.setMsgDescription(MessageConstants.C500DESC);
			apiResponse = new ProductRateCalculatorApiResponse(new ProductRateCalculatorResponse(header, msginfo, null));
		}

		response.setHeader(header);
		response.setMsgInfo(msginfo);
		apiResponse.setResponse(response);

		try {
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			logger.info("ResponseJson :: "+ow.writeValueAsString(apiResponse));
		}
		catch (Exception ex) {
			logger.error("Error While converting response data to json : "+ex);
		}
		return apiResponse;

	}

	private double rateCountForTCOT60(List<Map<String , String>> result,RequestPlan reqPlan,String ammount){
		double rate =0.0;
		try{
			for(Map<String , String> resultData :result){
				if(resultData.get("key0")!=null && ("TCOT60").equalsIgnoreCase(resultData.get("key0").trim())
						&& resultData.get("key3")!=null && resultData.get("key3").trim().equalsIgnoreCase(ammount)
						&& resultData.get("key7")!=null && resultData.get("key7").trim().equalsIgnoreCase(reqPlan.getPolicyTerm())
						&& resultData.get("key6")!=null && resultData.get("key6").trim().equalsIgnoreCase(("Y").equalsIgnoreCase(reqPlan.getSmoke().trim())?"S":"N")){
					if(resultData.get("key10")!=null && resultData.get("key10").trim().equalsIgnoreCase(reqPlan.getVariantId())){
						rate = resultData.get("key11") ==null?0.0:Double.valueOf(resultData.get("key11").trim());
						break;
					}
					else if(resultData.get("key12")!=null && resultData.get("key12").trim().equalsIgnoreCase(reqPlan.getVariantId())){
						rate = resultData.get("key13") == null?0.0:Double.valueOf(resultData.get("key13").trim());
						break;
					}
					else if(resultData.get("key14")!=null && resultData.get("key14").trim().equalsIgnoreCase(reqPlan.getVariantId()))		{
						rate = resultData.get("key15") == null?0.0:Double.valueOf(resultData.get("key15").trim());
						break;
					}
				}
			}
		}
		catch(Exception e){
			logger.error("Exception while rateCountForTCOT60 ::" +e );
		}
		return rate;

	}

	private  double rateCountForTCOTP2(List<Map<String , String>> result,RequestPlan reqPlan,String ammount){
		double rate =0.0;
		try{
			for(Map<String , String> resultData :result){
				if(resultData.get("key0")!=null && ("TCOTP2").equalsIgnoreCase(resultData.get("key0").trim())
						&& resultData.get("key3")!=null && resultData.get("key3").trim().equalsIgnoreCase(ammount)
						&& resultData.get("key7")!=null && resultData.get("key7").trim().equalsIgnoreCase(reqPlan.getPolicyTerm())
						&& resultData.get("key6")!=null && resultData.get("key6").trim().equalsIgnoreCase(("Y").equalsIgnoreCase(reqPlan.getSmoke().trim())?"S":"N")){
					if(resultData.get("key10")!=null && resultData.get("key10").trim().equalsIgnoreCase(reqPlan.getVariantId())){
						rate = resultData.get("key11") == null?0.0:Double.valueOf(resultData.get("key11").trim());
						break;
					}
					else if(resultData.get("key12")!=null && resultData.get("key12").trim().equalsIgnoreCase(reqPlan.getVariantId())){
						rate = resultData.get("key13") == null?0.0:Double.valueOf(resultData.get("key13").trim());
						break;
					}
					else if(resultData.get("key14")!=null && resultData.get("key14").trim().equalsIgnoreCase(reqPlan.getVariantId())){
						rate = resultData.get("key15") == null?0.0:Double.valueOf(resultData.get("key15").trim());
						break;
					}
				}
			}
		}

		catch(Exception e){
			logger.error("Exception while rateCountForTCOTP2 ::" +e );
		}
		return rate;

	}

	private  double rateCountForRiderVN05(List<PlanDetailBean> result,RequestRider reqRider,double amount){
		double riderRate =0.0;
		String ammountRider="";
		try{
			double premiumAmountRider = Double.compare(amount, 0.0D) != 0?amount:0D;
			if(premiumAmountRider >= 0 && premiumAmountRider <= Long.parseLong(environment.getProperty("productrate.premiumamount.range7"))){										  
				ammountRider=environment.getProperty("productrate.premiumamount.range7");
			}

			for(PlanDetailBean resultData :result){
				if(resultData.getRhBandAmount()!=null && resultData.getRhBandAmount().equalsIgnoreCase(ammountRider)
						&& resultData.getRtblMatXpryDur() !=null && resultData.getRtblMatXpryDur().equalsIgnoreCase(reqRider.getRiderTerm())){
					riderRate = resultData.getRtbl1rt() ==null ?0.0:Double.valueOf(resultData.getRtbl1rt());
					break;
				}
			}
		}
		catch(Exception e){
			logger.error("Exception while rateCountForRiderVN05 ::" +e );
		}
		return riderRate;

	}

	private double rateCountForRiderTCCIB(List<Map<String , String>> result,RequestRider reqRider){
		double riderRate =0.0;
		String ammountRider="";
		try{
			long premiumAmountRider=reqRider.getRiderSA()!=null?Long.valueOf(reqRider.getRiderSA().trim()):0;
			if(premiumAmountRider >= 0 && premiumAmountRider <= 1499999){										  
				ammountRider="1499999";
			}
			else if(premiumAmountRider >= 1500000 && premiumAmountRider <= 9999999999L){
				ammountRider="9999999999";
			}

			for(Map<String , String> resultData :result){
				if(resultData.get("key0")!=null && ("TCCIB").equalsIgnoreCase(resultData.get("key0").trim())
						&& resultData.get("key3")!=null && resultData.get("key3").trim().equalsIgnoreCase(ammountRider)
						&& resultData.get("key7")!=null && resultData.get("key7").trim().equalsIgnoreCase(reqRider.getRiderTerm())){
					riderRate = resultData.get("key16") == null?0.0:Double.valueOf(resultData.get("key16").trim());
					break;
				}
			}
		}
		catch(Exception e){
			logger.error("Exception while rateCountForRiderTCCIB ::" +e );
		}
		return riderRate;
	}

	private double rateCountForRiderDCCCP(List<PlanDetailBean> result,RequestRider reqRider, String discount){
		double riderRate =0.0;
		String ammountRider="";

		try{
			long premiumAmountRider = reqRider.getRiderSA() != null ? Long.valueOf(reqRider.getRiderSA().trim()):0;
			if(premiumAmountRider >= 0 && premiumAmountRider <= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range1"))){										  
				ammountRider = environment.getProperty("productrate.premiumamountrider.range1");
			}
			else if(premiumAmountRider >= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range2")) && premiumAmountRider <= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range3"))){
				ammountRider = environment.getProperty("productrate.premiumamountrider.range3");
			}
			else if(premiumAmountRider >= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range4")) && premiumAmountRider <= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range5"))){
				ammountRider = environment.getProperty("productrate.premiumamountrider.range5");
			}
			else if(premiumAmountRider >= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range6")) && premiumAmountRider <= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range7"))){
				ammountRider = environment.getProperty("productrate.premiumamountrider.range7");
			}
			else if(premiumAmountRider >= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range8")) && premiumAmountRider <= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range9"))){
				ammountRider = environment.getProperty("productrate.premiumamountrider.range9");
			}
			else if(premiumAmountRider >= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range10")) && premiumAmountRider <= Long.parseLong(environment.getProperty("productrate.premiumamountrider.range11"))){
				ammountRider = environment.getProperty("productrate.premiumamountrider.range11");
			}


			for(PlanDetailBean resultData :result){
				if(resultData.getRhBandAmount() != null && resultData.getRhBandAmount().equalsIgnoreCase(ammountRider)
						&& resultData.getRtblMatXpryDur() != null && resultData.getRtblMatXpryDur().equalsIgnoreCase(reqRider.getRiderTerm())
						&& resultData.getDiscountOption() != null && resultData.getDiscountOption().equalsIgnoreCase(discount))				{
					riderRate = resultData.getRtbl1rt() == null ?0.0:Double.valueOf(resultData.getRtbl1rt());
					break;
				}
			}
		}
		catch(Exception e){
			logger.error("Exception while rateCountForRiderTCCIB ::" +e );
		}
		return riderRate;
	}

	private double modeCountRider(RequestPlan reqPlan,String reqRider){
		double modeRate = 0.0;
		try{
			if(reqRider != null &&( planCodes.getPlancodelistTCCIBTNCIB().contains(reqRider))){
				switch(reqPlan.getMode().toUpperCase()){  
				case "ANNUAL": modeRate=1;break;  
				case "SEMI-ANNUAL": modeRate=1.026;break;  
				case "QUARTERLY":modeRate=1.044;break;  
				case "MONTHLY": modeRate=1.056;break; 
				} 
			}
			else if(reqRider!=null &&(planCodes.getPlancodelistTCCPABTNCPAB().contains(reqRider) || planCodes.getPlancodelistVN05VN04().contains(reqRider) )){
				switch(reqPlan.getMode().toUpperCase()){  
				case "ANNUAL": modeRate=1;break;  
				case "SEMI-ANNUAL": modeRate=1.04;break;  
				case "QUARTERLY":modeRate=1.06;break;  
				case "MONTHLY": modeRate=1.08;break; 
				} 
			}
			else if(reqRider!=null &&(planCodes.getPlancodelistDCCCP().contains(reqRider)  )){
				switch(reqPlan.getMode().toUpperCase()){  
				case "ANNUAL": modeRate=1;break;  
				case "SEMI-ANNUAL": modeRate=0.52;break;  
				case "QUARTERLY":modeRate=0.265;break;  
				case "MONTHLY": modeRate=0.09;break; 
				} 
			}
		}
		catch(Exception e){
			logger.error("some error occurs in modeCount ::" +e );
		}
		return modeRate;

	}

	private double modeCount(RequestPlan reqPlan)
	{
		double modeRate =0.0;
		try{
			if(reqPlan.getPlanId() !=null && (planCodes.getPlancodelistTCOT60TNOT60().contains(reqPlan.getPlanId().trim())  
					|| planCodes.getPlancodelistTCOTP2TNOTP2().contains(reqPlan.getPlanId().trim()) )){
				switch(reqPlan.getMode().toUpperCase()){  
				case "ANNUAL": modeRate=1;break;  
				case "SEMI-ANNUAL": modeRate=1.026;break;  
				case "QUARTERLY":modeRate=1.044;break;  
				case "MONTHLY": modeRate=1.056;break; 
				}  
			}

		}
		catch(Exception e){
			logger.error("Exception while modeCount ::" +e );
		}
		return modeRate;

	}

	private double modeCountValue(Double value,String reqRider){
		double result=0.0;
		try{
			switch(reqRider.toUpperCase()){  
			case "ANNUAL": result=value/1;break;  
			case "SEMI-ANNUAL": result=value/2;break;  
			case "QUARTERLY":result=value/4;break;  
			case "MONTHLY": result=value/12;break; 
			}  
		}
		catch(Exception e){
			logger.error("Exception while dividing by mode ::"+e  );
		}

		return result;

	}
	private Boolean validateHeader(ProductRateCalculatorApiRequest productRateCalculatorApiRequest){
		return (productRateCalculatorApiRequest != null 
				&& productRateCalculatorApiRequest.getRequest()!=null
				&& productRateCalculatorApiRequest.getRequest().getHeader() !=null
				&& productRateCalculatorApiRequest.getRequest().getHeader().getSoaAppId() !=null
				&& productRateCalculatorApiRequest.getRequest().getHeader().getSoaCorrelationId() !=null
				&& productRateCalculatorApiRequest.getRequest().getPayload() != null
				&& productRateCalculatorApiRequest.getRequest().getPayload().getReqPlan() != null 
				&& !productRateCalculatorApiRequest.getRequest().getPayload().getReqPlan().isEmpty());

	}
}
