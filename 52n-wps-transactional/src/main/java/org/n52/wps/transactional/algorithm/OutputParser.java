/***************************************************************
 This implementation provides a framework to publish processes to the
web through the  OGC Web Processing Service interface. The framework 
is extensible in terms of processes and data handlers. 

 Copyright (C) 2006 by con terra GmbH

 Authors: 
	Bastian Schaeffer, Institute for Geoinformatics, Muenster, Germany

 Contact: Albert Remke, con terra GmbH, Martin-Luther-King-Weg 24,
 48155 Muenster, Germany, 52n@conterra.de

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 version 2 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program (see gnu-gpl v2.txt); if not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA or visit the web page of the Free
 Software Foundation, http://www.fsf.org.

 ***************************************************************/


package org.n52.wps.transactional.algorithm;
import net.opengis.wps.x100.OutputDataType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;

import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntegerBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.io.datahandler.xml.AbstractXMLParser;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.util.BasicXMLTypeFactory;


public class OutputParser {
	
	
	/**
	 * Handles the ComplexValueReference
	 * @param input The client input
	 * @throws ExceptionReport If the input (as url) is invalid, or there is an error while parsing the XML.
	 */
	protected  static String handleComplexValueReference(OutputDataType output) throws ExceptionReport{
		return output.getReference().getHref();
		
	}
	
	/**
	 * Handles the complexValue, which in this case should always include XML 
	 * which can be parsed into a FeatureCollection.
	 * @param input The client input
	 * @throws ExceptionReport If error occured while parsing XML
	 */
	protected static Object handleComplexValue(OutputDataType output, ProcessDescriptionType processDescription) throws ExceptionReport{
		String outputID = output.getIdentifier().getStringValue();
		String complexValue = output.getData().getComplexData().toString();
		OutputDescriptionType outputDesc = null;
		for(OutputDescriptionType tempDesc : processDescription.getProcessOutputs().getOutputArray()) {
			if((tempDesc.getIdentifier().getStringValue().startsWith(outputID))) {
				outputDesc = tempDesc;
				break;
			}
		}

		if(outputDesc == null) {
			throw new RuntimeException("output cannot be found in description for " + processDescription.getIdentifier().getStringValue() + "," + outputID);
		}
		
		String schema = output.getData().getComplexData().getSchema();
		String encoding = output.getData().getComplexData().getEncoding();
		String format = output.getData().getComplexData().getMimeType();
		if(schema == null) {
			schema = outputDesc.getComplexOutput().getDefault().getFormat().getSchema();
		}
		if(format == null) {
			format = outputDesc.getComplexOutput().getDefault().getFormat().getMimeType();
		}
		if(encoding == null) {
			encoding = outputDesc.getComplexOutput().getDefault().getFormat().getEncoding();
		}
		
		Class outputDataType = determineOutputDataType(outputID, outputDesc);
		
		IParser parser = ParserFactory.getInstance().getParser(schema, format, encoding, outputDataType);
		if(parser == null) {
			parser = ParserFactory.getInstance().getSimpleParser();
		}
		Object collection = null;
		if(parser instanceof AbstractXMLParser) {
			try {
				collection = ((AbstractXMLParser)parser).parseXML(complexValue);
			}
			catch(RuntimeException e) {
				throw new ExceptionReport("Error occured, while XML parsing", 
						ExceptionReport.NO_APPLICABLE_CODE, e);
			}
		}
		else {
			throw new ExceptionReport("parser does not support operation: " + parser.getClass().getName(), ExceptionReport.INVALID_PARAMETER_VALUE);
		}
		return collection;
	}
	
	

	private static Class determineOutputDataType(String outputID, OutputDescriptionType output) {
			
		if(output.isSetLiteralOutput()){
			String datatype = output.getLiteralOutput().getDataType().getStringValue();
			if(datatype.contains("tring")){
				return LiteralStringBinding.class;
			}
			if(datatype.contains("ollean")){
				return LiteralBooleanBinding.class;
			}
			if(datatype.contains("loat") || datatype.contains("ouble")){
				return LiteralDoubleBinding.class;
			}
			if(datatype.contains("nt")){
				return LiteralIntegerBinding.class;
			}
		}
		if(output.isSetComplexOutput()){
			String mimeType = output.getComplexOutput().getDefault().getFormat().getMimeType();
			if(mimeType.contains("xml") || (mimeType.contains("XML"))){
				return GTVectorDataBinding.class;
			}else{
				return GTRasterDataBinding.class;
			}
		}
		
		throw new RuntimeException("Could not determie internal inputDataType");
	}

	protected static Object handleLiteralValue(OutputDataType output) throws ExceptionReport {
		
		String parameter = output.getData().getLiteralData().getStringValue();
		String xmlDataType = output.getData().getLiteralData().getDataType();
		Object parameterObj = null;
		try {
			parameterObj = BasicXMLTypeFactory.getBasicJavaObject(xmlDataType, parameter);
		}
		catch(RuntimeException e) {
			throw new ExceptionReport("The passed parameterValue: " + parameter + ", but should be of type: " + xmlDataType, ExceptionReport.INVALID_PARAMETER_VALUE);
		}
		if(parameterObj == null) {
			throw new ExceptionReport("XML datatype as LiteralParameter is not supported by the server: dataType " + xmlDataType, 
					ExceptionReport.INVALID_PARAMETER_VALUE);
		}
		return parameterObj;
		
	}
	
	/**
	 * Handles BBoxValue
	 * @param input The client input
	 */
	protected static Object handleBBoxValue(OutputDataType input) throws ExceptionReport{
		//String inputID = input.getIdentifier().getStringValue();
		throw new ExceptionReport("BBox is not supported", ExceptionReport.OPERATION_NOT_SUPPORTED);
	}

}