package io.mosip.pms.policy.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import io.mosip.pms.common.request.dto.RequestWrapperV2;
import io.mosip.pms.common.response.dto.ResponseWrapperV2;
import io.mosip.pms.common.util.RequestValidator;
import io.mosip.pms.policy.util.PolicyUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.mosip.kernel.core.http.ResponseFilter;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.pms.common.dto.FilterValueDto;
import io.mosip.pms.common.dto.PageResponseDto;
import io.mosip.pms.common.dto.PolicyFilterValueDto;
import io.mosip.pms.common.dto.PolicySearchDto;
import io.mosip.pms.common.dto.SearchAuthPolicy;
import io.mosip.pms.common.dto.SearchDto;
import io.mosip.pms.common.dto.PageResponseV2Dto;
import io.mosip.pms.common.entity.PolicyGroup;
import io.mosip.pms.common.util.PMSLogger;
import io.mosip.pms.policy.dto.FilterResponseCodeDto;
import io.mosip.pms.policy.dto.KeyValuePair;
import io.mosip.pms.policy.dto.PolicyCreateRequestDto;
import io.mosip.pms.policy.dto.PolicyCreateResponseDto;
import io.mosip.pms.policy.dto.PolicyDetailsDto;
import io.mosip.pms.policy.dto.PolicyGroupCreateRequestDto;
import io.mosip.pms.policy.dto.PolicyGroupCreateResponseDto;
import io.mosip.pms.policy.dto.PolicyGroupUpdateRequestDto;
import io.mosip.pms.policy.dto.PolicyManageEnum;
import io.mosip.pms.policy.dto.PolicyResponseDto;
import io.mosip.pms.policy.dto.PolicyStatusUpdateRequestDto;
import io.mosip.pms.policy.dto.PolicyStatusUpdateResponseDto;
import io.mosip.pms.policy.dto.PolicyUpdateRequestDto;
import io.mosip.pms.policy.dto.PolicyWithAuthPolicyDto;
import io.mosip.pms.policy.dto.RequestWrapper;
import io.mosip.pms.policy.dto.ResponseWrapper;
import io.mosip.pms.policy.dto.PolicyGroupDto;
import io.mosip.pms.policy.dto.PolicySummaryDto;
import io.mosip.pms.policy.dto.PolicyFilterDto;
import io.mosip.pms.policy.dto.DeactivatePolicyResponseDto;
import io.mosip.pms.policy.dto.DeactivatePolicyGroupResponseDto;
import io.mosip.pms.policy.dto.DeactivateRequestDto;
import io.mosip.pms.policy.service.PolicyManagementService;
import io.mosip.pms.policy.util.AuditUtil;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;


@RestController
@RequestMapping(value = "/policies")
@Api(tags = { "policy management controller " })
public class PolicyManagementController {

	private static final Logger logger = PMSLogger.getLogger(PolicyManagementController.class);

	@Value("${mosip.pms.api.id.deactivate.policy.patch}")
	private  String patchDeactivatePolicy;

	@Value("${mosip.pms.api.id.deactivate.policy.group.patch}")
	private String patchDeactivatePolicyGroupId;

	@Autowired
	private PolicyManagementService policyManagementService;
	
	@Autowired
	AuditUtil auditUtil;

	@Autowired
	RequestValidator requestValidator;

	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostpoliciesgroupnew())")
	@PostMapping(value = "/group/new")
	@Operation(summary = "Service to create a new policy group", description = "Service to craete a new policy group")
	public ResponseWrapper<PolicyGroupCreateResponseDto> definePolicyGroup(
			@RequestBody @Valid RequestWrapper<PolicyGroupCreateRequestDto> createRequest) {
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		auditUtil.setAuditRequestDto(PolicyManageEnum.CREATE_POLICY_GROUP, createRequest.getRequest().getName(), "policyGroupName");
		PolicyGroupCreateResponseDto responseDto = policyManagementService.createPolicyGroup(createRequest.getRequest());
		ResponseWrapper<PolicyGroupCreateResponseDto> response = new ResponseWrapper<>();
		response.setResponse(responseDto);
		response.setId(createRequest.getId());
		response.setVersion(createRequest.getVersion());
		return response;		
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getPutpoliciesgrouppolicygroupid())")
	@PutMapping(value = "/group/{policygroupId}")
	@Operation(summary = "Service to update a policy group", description = "Service to update a policy group")
	public ResponseWrapper<PolicyGroupCreateResponseDto> updatePolicyGroup(@PathVariable String policygroupId,
			@RequestBody @Valid RequestWrapper<PolicyGroupUpdateRequestDto> createRequest) {
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		auditUtil.setAuditRequestDto(PolicyManageEnum.UPDATE_POLICY_GROUP, createRequest.getRequest().getName(), "policyGroupName");
		PolicyGroupCreateResponseDto responseDto = policyManagementService.updatePolicyGroup(createRequest.getRequest(), policygroupId);
		ResponseWrapper<PolicyGroupCreateResponseDto> response = new ResponseWrapper<>();
		response.setResponse(responseDto);
		response.setId(createRequest.getId());
		response.setVersion(createRequest.getVersion());		
		return response;		
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostpolicies())")
	@PostMapping
	@Operation(summary = "Service to create a new auth/datashare/ credential policy", description = "Service to create a new auth/datashare/ credential policy")
	public ResponseWrapper<PolicyCreateResponseDto> definePolicy(
			@RequestBody @Valid RequestWrapper<PolicyCreateRequestDto> createRequest) throws Exception {
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		ResponseWrapper<PolicyCreateResponseDto> response = new ResponseWrapper<PolicyCreateResponseDto>();
		auditUtil.setAuditRequestDto(PolicyManageEnum.CREATE_POLICY, createRequest.getRequest().getName(), "policyName");
		PolicyCreateResponseDto responseDto = policyManagementService.
				createPolicies(createRequest.getRequest());		
		response.setId(createRequest.getId());
		response.setVersion(createRequest.getVersion());
		response.setResponse(responseDto);		
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostpoliciespolicyidgrouppublish())")
	@PostMapping(value = "/{policyId}/group/{policygroupId}/publish")
	@Operation(summary = "Service to publish policy", description = "Service to publish policy")
	public ResponseWrapper<PolicyResponseDto> publishPolicy(@PathVariable @Valid String policygroupId,
			@PathVariable @Valid String policyId) throws JsonParseException, JsonMappingException, IOException {
		ResponseWrapper<PolicyResponseDto> response = new ResponseWrapper<PolicyResponseDto>();
		auditUtil.setAuditRequestDto(PolicyManageEnum.PUBLISH_POLICY, policyId, "policyId");
		PolicyResponseDto responseDto = policyManagementService.publishPolicy(policygroupId, policyId);
		response.setResponse(responseDto);
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getPutpoliciespolicyid())")
    @PutMapping(value ="/{policyId}")
	@Operation(summary = "Service to update policy details", description = "Service to update policy details")
	public ResponseWrapper<PolicyCreateResponseDto> updatePolicyDetails(
			@RequestBody @Valid RequestWrapper<PolicyUpdateRequestDto> updateRequestDto, @PathVariable String policyId)
			throws Exception {
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		auditUtil.setAuditRequestDto(PolicyManageEnum.UPDATE_POLICY, updateRequestDto.getRequest().getName(), "policyName");
		ResponseWrapper<PolicyCreateResponseDto> response = new ResponseWrapper<PolicyCreateResponseDto>();
		PolicyCreateResponseDto responseDto = policyManagementService.updatePolicies(updateRequestDto.getRequest(),
				policyId);
		response.setResponse(responseDto);
		response.setId(updateRequestDto.getId());
		response.setVersion(updateRequestDto.getVersion());
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getPatchpoliciespolicyidgrouppolicygroupid())")
	@PatchMapping(value = "/{policyId}/group/{policygroupId}")
	@Operation(summary = "Service to update policy status", description = "Service to update policy status")
	public ResponseWrapper<PolicyStatusUpdateResponseDto> updatePolicyStatus(
			@RequestBody RequestWrapper<PolicyStatusUpdateRequestDto> requestDto, @PathVariable String policygroupId,
			@PathVariable String policyId) throws Exception {
		PolicyStatusUpdateRequestDto statusUpdateRequest = requestDto.getRequest();
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		auditUtil.setAuditRequestDto(PolicyManageEnum.UPDATE_POLICY_STATUS, policyId, "policyId");
		ResponseWrapper<PolicyStatusUpdateResponseDto> response =  policyManagementService.
				updatePolicyStatus(statusUpdateRequest,policygroupId,policyId);	
		response.setId(requestDto.getId());
		response.setVersion(requestDto.getVersion());
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetpolicies())")
	@GetMapping
	@Operation(summary = "Service to get policies", description = "Service to get policies")
	public ResponseWrapper<List<PolicyResponseDto>> getPolicies()
			throws FileNotFoundException, IOException, ParseException {
		ResponseWrapper<List<PolicyResponseDto>> response = new ResponseWrapper<>();
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		response.setResponse(policyManagementService.findAllPolicies());
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetpoliciespolicyid())")
	@GetMapping(value = "/{policyId}")
	@Operation(summary = "Service to get policy", description = "Service to get policy")
	public ResponseWrapper<PolicyResponseDto> getPolicy(@PathVariable String policyId) throws Exception {
		ResponseWrapper<PolicyResponseDto> response = new ResponseWrapper<>();
		logger.info("Calling PolicyManagementService from PolicyManageController.");
		PolicyResponseDto responseDto = policyManagementService.findPolicy(policyId);
		response.setResponse(responseDto);
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetpoliciespolicyid())")
	@GetMapping(value = "/{policyId}/partner/{partnerId}")
	@Operation(summary = "Service to get policy for given partner and policy id", description = "Service to get policy for given partner and policy id")
	public ResponseWrapper<PolicyResponseDto> getPartnersPolicy(@PathVariable String partnerId,
			@PathVariable String policyId) throws JsonParseException, JsonMappingException, IOException {
		ResponseWrapper<PolicyResponseDto> response = new ResponseWrapper<>();
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		PolicyResponseDto policyGroup = policyManagementService.getPartnerMappedPolicy(partnerId, policyId);
		response.setResponse(policyGroup);
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetpoliciesgrouppolicygroupid())")
	@GetMapping(value = "/group/{policygroupId}")
	@Operation(summary = "Service to get policy group", description = "Service to get policy group")
	public ResponseWrapper<PolicyWithAuthPolicyDto> getPolicyGroup(@PathVariable String policygroupId)
			throws JsonParseException, JsonMappingException, IOException {
		ResponseWrapper<PolicyWithAuthPolicyDto> response = new ResponseWrapper<>();
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		response.setResponse(policyManagementService.getPolicyGroupPolicy(policygroupId));
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetpoliciesgroupall())")
	@GetMapping(value = "/group/all")
	@Operation(summary = "Service to get all policy groups", description = "Service to all policy groups")
	public ResponseWrapper<List<PolicyWithAuthPolicyDto>> getPolicyGroup()
			throws JsonParseException, JsonMappingException, IOException {
		ResponseWrapper<List<PolicyWithAuthPolicyDto>> response = new ResponseWrapper<>();
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		response.setResponse(policyManagementService.getPolicyGroup());
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@ResponseFilter
	@PostMapping("/group/search")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostpoliciesgroupsearch())")
	@Operation(summary = "Service to search policy group", description = "Service to search policy group")
	public ResponseWrapper<PageResponseDto<PolicyGroup>> searchPolicyGroup(
			@RequestBody @Valid RequestWrapper<SearchDto> request) {
		ResponseWrapper<PageResponseDto<PolicyGroup>> responseWrapper = new ResponseWrapper<>();
		auditUtil.setAuditRequestDto(PolicyManageEnum.SEARCH_POLICY_GROUP);
		responseWrapper.setResponse(policyManagementService.searchPolicyGroup(request.getRequest()));
		return responseWrapper;
	}

	@ResponseFilter	
	@PostMapping("/search")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostpoliciessearch())")
	@Operation(summary = "Service to search policy", description = "Service to search policy")
	public ResponseWrapper<PageResponseDto<SearchAuthPolicy>> searchPolicy(
			@RequestBody @Valid RequestWrapper<PolicySearchDto> request) {
		ResponseWrapper<PageResponseDto<SearchAuthPolicy>> responseWrapper = new ResponseWrapper<>();
		auditUtil.setAuditRequestDto(PolicyManageEnum.SEARCH_POLICY);
		responseWrapper.setResponse(policyManagementService.searchPolicy(request.getRequest()));
		return responseWrapper;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetpoliciesconfigkey())")
	@GetMapping(value = "/config/{key}")
	@Operation(summary = "Service to get value for a given config key", description = "Service to get value for a given config key")
	public ResponseWrapper<KeyValuePair<String, Object>> getValueForKey(@PathVariable String key) {
		ResponseWrapper<KeyValuePair<String, Object>> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(policyManagementService.getValueForKey(key));
		return responseWrapper;
	}

	@PostMapping("/group/filtervalues")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostpoliciesgroupfiltervalues())")	@Operation(summary = "Service to filter policy groups", description = "Service to filter policy groups")
	public ResponseWrapper<FilterResponseCodeDto> policyGroupFilterValues(
			@RequestBody @Valid RequestWrapper<FilterValueDto> requestWrapper) {
		ResponseWrapper<FilterResponseCodeDto> responseWrapper = new ResponseWrapper<>();
		auditUtil.setAuditRequestDto(PolicyManageEnum.FILTERVALUES_POLICY_GROUP);
		responseWrapper.setResponse(policyManagementService.policyGroupFilterValues(requestWrapper.getRequest()));
		return responseWrapper;
	}

	@PostMapping("/filtervalues")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostpoliciesfiltervalues())")
	@Operation(summary = "Service to filter policy details", description = "Service to filter policy details")
	public ResponseWrapper<FilterResponseCodeDto> policyFilterValues(
			@RequestBody @Valid RequestWrapper<PolicyFilterValueDto> requestWrapper) {
		ResponseWrapper<FilterResponseCodeDto> responseWrapper = new ResponseWrapper<>();
		auditUtil.setAuditRequestDto(PolicyManageEnum.FILTERVALUES_POLICY);
		responseWrapper.setResponse(policyManagementService.policyFilterValues(requestWrapper.getRequest()));
		return responseWrapper;
	}
	
	@GetMapping("/active/group/{groupName}")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetactivegroupgroupname())")
	@Operation(summary = "Service to get active policy details for policy group name", description = "Service to get active policy details for policy group name")
	public ResponseWrapper<List<PolicyDetailsDto>> getPoliciesByGroupName(@PathVariable String groupName) {
		ResponseWrapper<List<PolicyDetailsDto>> response = new ResponseWrapper<>();
		logger.info("Calling PolicyManagementService from PolicyManagementController.");
		response.setResponse(policyManagementService.getActivePolicyDetailsByGroupName(groupName));
		logger.info("Returning response from PolicyManagementController.");
		return response;
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetpolicygroups())")
	@GetMapping(value = "/policy-groups")
	@Operation(summary = "This endpoint retrieves details about all active Policy Groups", 
	description = "Available since release-1.2.2.0.")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))})
	public ResponseWrapperV2<List<PolicyGroupDto>> getPolicyGroups() throws JsonParseException, JsonMappingException, IOException {
		return policyManagementService.getPolicyGroups();
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetallpolicies())")
	@GetMapping(value = "/v2")
	@Operation(summary = "This endpoint retrieves the list of all Policies",
	description = "Available since release-1.2.2.0. It is configured for the POLICYMANAGER or PARTNER_ADMIN roles.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
	})
	public ResponseWrapperV2<PageResponseV2Dto<PolicySummaryDto>> getAllPolicies(
			@RequestParam(value = "sortFieldName", required = false) String sortFieldName,
			@RequestParam(value = "sortType", required = false) String sortType, // e.g., ASC or DESC
			@RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
			@RequestParam(value = "pageSize", defaultValue = "8") int pageSize,
			@RequestParam(value = "policyType", required = false) String policyType,
			@RequestParam(value = "policyId", required = false) String policyId,
			@RequestParam(value = "policyName", required = false) String policyName,
			@RequestParam(value = "policyDescription", required = false) String policyDescription,
			@RequestParam(value = "policyGroupName", required = false) String policyGroupName,
			@Parameter(
					description = "Status of policy",
					in = ParameterIn.QUERY,
					schema = @Schema(allowableValues = {"activated", "deactivated", "draft"})
			)
			@RequestParam(value = "status", required = false) String status) {

		PolicyUtil.validateGetAllPoliciesRequestParameters(sortFieldName, sortType, pageNo, pageSize);
		PolicyFilterDto filterDto = new PolicyFilterDto();
		if (policyType != null) {
			filterDto.setPolicyType(policyType.toLowerCase());
		}
		if (policyId != null) {
			filterDto.setPolicyId(policyId.toLowerCase());
		}
		if (policyName != null) {
			filterDto.setPolicyName(policyName.toLowerCase());
		}
		if (policyDescription != null) {
			filterDto.setPolicyDescription(policyDescription.toLowerCase());
		}
		if (policyGroupName != null) {
			filterDto.setPolicyGroupName(policyGroupName.toLowerCase());
		}
		if (status != null) {
			filterDto.setStatus(status);
		}
		return policyManagementService.getAllPolicies(sortFieldName, sortType, pageNo, pageSize, filterDto);
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getPatchdeactivatepolicy())")
	@PatchMapping(value = "/{policyId}")
	@Operation(summary = "Available since release-1.2.2.0. This endpoint deactivates a policy based on the Policy Id", description = "This endpoint deactivates a policy based on the Policy Id, accessible only by Partner Admin. It checks if any policy requests are associated with the policy: it can be deactivated if there are no requests or if there are rejected requests. It cannot be deactivated if there are approved or pending requests, returning error codes PMS_POL_063 or PMS_POL_064, respectively. This endpoint is configured for the POLICYMANAGER or PARTNER_ADMIN roles.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
	})
	public ResponseWrapperV2<DeactivatePolicyResponseDto> deactivatePolicy(@PathVariable("policyId") @NotBlank String policyId, @RequestBody @Valid RequestWrapperV2<DeactivateRequestDto>
			requestWrapper) {
		Optional<ResponseWrapperV2<DeactivatePolicyResponseDto>> validationResponse = requestValidator.validate(patchDeactivatePolicy, requestWrapper);
		if (validationResponse.isPresent()) {
			return validationResponse.get();
		}
		return policyManagementService.deactivatePolicy(policyId, requestWrapper.getRequest());
	}

	@PreAuthorize("hasAnyRole(@authorizedRoles.getPatchdeactivatepolicygroup())")
	@PatchMapping(value = "/group/{policyGroupId}")
	@Operation(summary = "This endpoint allows Partner Admin users to deactivate a Policy Group based on the Policy Group Id.",
			description = "Available since release-1.2.2.0. It is configured for the POLICYMANAGER or PARTNER_ADMIN roles.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
	})
	public ResponseWrapperV2<DeactivatePolicyGroupResponseDto> deactivatePolicyGroup(@PathVariable("policyGroupId") @NotBlank String policyGroupId, @RequestBody @Valid RequestWrapperV2<DeactivateRequestDto>
			requestWrapper) {
		Optional<ResponseWrapperV2<DeactivatePolicyGroupResponseDto>> validationResponse = requestValidator.validate(patchDeactivatePolicyGroupId, requestWrapper);
		if (validationResponse.isPresent()) {
			return validationResponse.get();
		}
		return policyManagementService.deactivatePolicyGroup(policyGroupId, requestWrapper.getRequest());
	}
}
