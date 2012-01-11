/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.module;

/**
 * @author Sebastian Baechle
 * 
 */
public class DecimalFormat {
	private String decimalSeparator = ".";
	private String groupingSeparator = ",";
	private String infinity = "Infinity";
	private String minusSign = "-";
	private String naN = "NaN";
	private String percent = "%";
	private String perMille = "\u8240";
	private String zeroDigit = "0";
	private String digitSign = "#";
	private String patternSeparator = ";";

	public String getDecimalSeparator() {
		return decimalSeparator;
	}

	public void setDecimalSeparator(String decimalSeparator) {
		this.decimalSeparator = decimalSeparator;
	}

	public String getGroupingSeparator() {
		return groupingSeparator;
	}

	public void setGroupingSeparator(String groupingSeparator) {
		this.groupingSeparator = groupingSeparator;
	}

	public String getInfinity() {
		return infinity;
	}

	public void setInfinity(String infinity) {
		this.infinity = infinity;
	}

	public String getMinusSign() {
		return minusSign;
	}

	public void setMinusSign(String minusSign) {
		this.minusSign = minusSign;
	}

	public String getNaN() {
		return naN;
	}

	public void setNaN(String naN) {
		this.naN = naN;
	}

	public String getPercent() {
		return percent;
	}

	public void setPercent(String percent) {
		this.percent = percent;
	}

	public String getPerMille() {
		return perMille;
	}

	public void setPerMille(String perMille) {
		this.perMille = perMille;
	}

	public String getZeroDigit() {
		return zeroDigit;
	}

	public void setZeroDigit(String zeroDigit) {
		this.zeroDigit = zeroDigit;
	}

	public String getDigitSign() {
		return digitSign;
	}

	public void setDigitSign(String digitSign) {
		this.digitSign = digitSign;
	}

	public String getPatternSeparator() {
		return patternSeparator;
	}

	public void setPatternSeparator(String patternSeparator) {
		this.patternSeparator = patternSeparator;
	}
}
