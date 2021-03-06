/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.metaclass

import grails.web.UrlConverter
import groovy.transform.CompileStatic
import org.grails.web.mapping.UrlMappingUtils
import org.springframework.beans.MutablePropertyValues
import org.springframework.validation.DataBinder

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.grails.web.mapping.ForwardUrlMappingInfo
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext

/**
 * Implements performing a forward.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
@CompileStatic
class ForwardMethod {

    public static final String IN_PROGRESS = "org.codehaus.groovy.grails.FORWARD_IN_PROGRESS"
    public static final String CALLED = "org.codehaus.groovy.grails.FORWARD_CALLED"

    private UrlConverter urlConverter

    String forward(HttpServletRequest request, HttpServletResponse response, Map params) {
        def urlInfo = new ForwardUrlMappingInfo()
        DataBinder binder = new DataBinder(urlInfo)
        binder.bind(new MutablePropertyValues(params))

        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request)

        if (webRequest) {
            def controllerName
            if(params.controller) {
                controllerName = params.controller
            } else {
                controllerName = webRequest.controllerName
            }
            
            if(controllerName) {
                def convertedControllerName = convert(webRequest, controllerName.toString())
                webRequest.controllerName = convertedControllerName
            }
            urlInfo.controllerName = webRequest.controllerName
            
            if(params.action) {
                urlInfo.actionName = convert(webRequest, params.action.toString())
            }
            
            if(params.namespace) {
                urlInfo.namespace = params.namespace
            }
            
            if(params.plugin) {
                urlInfo.pluginName = params.plugin
            }
        }
         
        def model = params.model instanceof Map ? params.model : Collections.EMPTY_MAP
        request.setAttribute(IN_PROGRESS, true)
        String uri = UrlMappingUtils.forwardRequestForUrlMappingInfo(request, response, urlInfo, (Map)model, true)
        request.setAttribute(CALLED, true)
        return uri
    }

    void setUrlConverter(UrlConverter urlConverter) {
        this.urlConverter = urlConverter
    }

    private UrlConverter lookupUrlConverter(GrailsWebRequest webRequest) {
        if (!urlConverter) {
            ApplicationContext applicationContext = webRequest?.getApplicationContext()
            if (applicationContext) {
                urlConverter = applicationContext.getBean("grailsUrlConverter", UrlConverter)
            }
        }

        urlConverter
    }

    private String convert(GrailsWebRequest webRequest, String value) {
        UrlConverter urlConverter = lookupUrlConverter(webRequest)
        (urlConverter) ? urlConverter.toUrlElement(value) : value
    }
}
