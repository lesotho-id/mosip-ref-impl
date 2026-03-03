/**
 *
 */
package io.mosip.kernel.smsserviceprovider.citsms.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import io.mosip.kernel.core.notification.exception.InvalidNumberException;
import io.mosip.kernel.core.notification.model.SMSResponseDto;
import io.mosip.kernel.core.notification.spi.SMSServiceProvider;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.smsserviceprovider.citsms.constant.SmsExceptionConstant;
import io.mosip.kernel.smsserviceprovider.citsms.constant.SmsPropertyConstant;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ritesh Sinha
 * @since 1.0.0
 */
@Component
public class SMSServiceProviderImpl implements SMSServiceProvider {

    @Autowired
    RestTemplate restTemplate;

    @Value("${mosip.kernel.sms.enabled:false}")
    boolean smsEnabled;

    @Value("${mosip.kernel.sms.country.code}")
    String countryCode;

    @Value("${mosip.kernel.sms.number.length}")
    int numberLength;

    @Value("${mosip.kernel.sms.api}")
    String api;

    @Value("${mosip.kernel.sms.authkey:null}")
    String authkey;

    @Override
    public SMSResponseDto sendSms(String contactNumber, String message) {
        SMSResponseDto smsResponseDTO = new SMSResponseDto();

        // Remove + if present before validation
        contactNumber = contactNumber.replace("+", "").trim();
        validateInput(contactNumber);

        if (!smsEnabled) {
            smsResponseDTO.setStatus("success");
            smsResponseDTO.setMessage("SMS disabled");
            return smsResponseDTO;
        }
        try {

            // Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", authkey);

            System.out.println("authkey : " + authkey);
            // Prepare full international number
            String formattedCountryCode = countryCode.trim();
            if (!formattedCountryCode.startsWith("+")) {
                formattedCountryCode = "+" + formattedCountryCode;
            }

            String fullNumber = formattedCountryCode + contactNumber;
            System.out.println("fullNumber : " + fullNumber);
            // Prepare Body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("to", fullNumber);
            requestBody.put("message", message);

            HttpEntity<Map<String, String>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(api, entity, String.class);

            smsResponseDTO.setStatus("success");
            smsResponseDTO.setMessage(response.getBody());

            return smsResponseDTO;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }

    private void validateInput(String contactNumber) {
        if (!StringUtils.isNumeric(contactNumber) || contactNumber.length() < numberLength
                || contactNumber.length() > numberLength) {
            throw new InvalidNumberException(SmsExceptionConstant.SMS_INVALID_CONTACT_NUMBER.getErrorCode(),
                    SmsExceptionConstant.SMS_INVALID_CONTACT_NUMBER.getErrorMessage() + numberLength
                            + SmsPropertyConstant.SUFFIX_MESSAGE.getProperty());
        }
    }
}