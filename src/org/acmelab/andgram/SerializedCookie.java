/*
 * Copyright 2011, Mark L. Chang <mark.chang@gmail.com>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Mark L. Chang ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MARK L. CHANG OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Mark L. Chang.
 */

package org.acmelab.andgram;

import org.apache.http.cookie.Cookie;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: mchang
 * Date: 3/28/11
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class SerializedCookie implements Serializable {

	private static final long serialVersionUID = 5327445113190674523L; //arbitrary

	private String name;
	private String value;
	private String domain;

	public SerializedCookie(Cookie cookie){
		this.name = cookie.getName();
		this.value = cookie.getValue();
		this.domain = cookie.getDomain();
	}

	public String getName(){
		return name;
	}

	public String getValue(){
		return value;
	}
	public String getDomain(){
		return domain;
	}
}
