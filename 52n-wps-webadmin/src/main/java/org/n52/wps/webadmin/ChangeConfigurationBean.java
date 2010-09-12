/***************************************************************
This implementation provides a framework to publish processes to the
web through the  OGC Web Processing Service interface. The framework
is extensible in terms of processes and data handlers. It is compliant
to the WPS version 0.4.0 (OGC 05-007r4).

Copyright (C) 2007 by con terra GmbH

Authors:
Florian van Keulen, ITC Student, ITC Enschede, the Netherlands


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
package org.n52.wps.webadmin;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.ArrayList;
import org.n52.wps.WPSConfigurationDocument;
import org.n52.wps.WPSConfigurationDocument.WPSConfiguration;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.ServerDocument.Server;
import org.n52.wps.AlgorithmRepositoryListDocument.AlgorithmRepositoryList;
import org.n52.wps.ParserListDocument.ParserList;
import org.n52.wps.GeneratorListDocument.GeneratorList;
import org.n52.wps.RepositoryDocument.Repository;
import org.n52.wps.GeneratorDocument.Generator;
import org.n52.wps.ParserDocument.Parser;
import org.n52.wps.PropertyDocument.Property;
import org.n52.wps.DatahandlersDocument.Datahandlers;
import org.apache.log4j.Logger;

/**
 * This Bean changes the WPSConfiguration of the Application by processing formdata
 * @author Florian van Keulen
 */
public class ChangeConfigurationBean {
    private static transient Logger LOGGER = Logger.getLogger(ChangeConfigurationBean.class);

    /**
     * Types represents the different types of the Entries to proceed
     */
    private enum Types {Server, Repository, Parser, Generator, Formelement};
    
    private WPSConfigurationDocument wpsConfigurationDocument;
    private WPSConfiguration wpsConfiguration;
    
    private AlgorithmRepositoryList repositoryList;
    private ParserList parserList;
    private GeneratorList generatorList;
    private Server server;

    private String serializedWPSConfiguraton = "";

    public void setSerializedWPSConfiguraton(String data){
        serializedWPSConfiguraton = data;
        processFormData(data);
        LOGGER.info("Saved and Activated new configuration!");
    }

    public String getSerializedWPSConfiguraton (){
        return serializedWPSConfiguraton;
    }

    /**
     * Processes the stringified formdata and updates the WPSConfiguration XML Bean
     * of the Application.
     * it is important, that the form input names are in a specific format.
     * Incorrect form input names will not processed.
     *
     * format:
     *   for a item:
     *   [Repository|Parser|Generator]-[number]_[Name|Class]
     *   for a Property:
     *   [Repository|Parser|Generator]-[number]_Property-[number]_[Name|Value]
     *
     * examples:
     *   items:
     *   Repository-13_Name
     *   Repository-13_Class
     *   properties:
     *   Parser-9_Property-14_Name
     *   Parser-9_Property-14_Value
     *
     *
     * @param formData stringified form data
     */
    private void processFormData(String formData) {
        // get WPSConfiguration XML Bean and create new Lists to hold the data
        wpsConfigurationDocument = WPSConfigurationDocument.Factory.newInstance();
        wpsConfiguration = wpsConfigurationDocument.addNewWPSConfiguration();
        server = wpsConfiguration.addNewServer();
        server.addNewDatabase();
        repositoryList = wpsConfiguration.addNewAlgorithmRepositoryList();
        Datahandlers datahandlers = wpsConfiguration.addNewDatahandlers();
        parserList = datahandlers.addNewParserList();
        generatorList = datahandlers.addNewGeneratorList();

        //temporary maps to store data in order to proceed
        HashMap<String, String> serverValues = new HashMap<String, String>();
        HashMap<String, String> repositoryValues = new HashMap<String, String>();
        HashMap<String, String> parserValues = new HashMap<String, String>();
        HashMap<String, String> generatorValues = new HashMap<String, String>();

        // split the stringified formdata into an array
        String[] dataArr = formData.split("&");

        String processingItemName = null;
        Types processingItemType = null;
        String[] formName;

        // main loop
        // loops over all entries and add them to the corresponding Map
        // if a item is completely proceed, it adds this to the WPSConfiguration
        for (int i = 0; i < dataArr.length; i++) {
            String dataStr = dataArr[i];
            // splits name and value
            String[] entryArr = dataStr.split("=");
            // extracts the actual processing item type (enum Types)
            processingItemType = Types.valueOf(entryArr[0].split("-", 2)[0]);

            // which type is the entry
            switch (processingItemType) {

                case Server:
                    serverValues.put(entryArr[0].split("-", 2)[1], entryArr[1]);
                    break;

                case Repository:
                    // splits the formname to the type with number
                    // as identifier
                    formName = entryArr[0].split("_", 2);

                    // the first item to proceed
                    if (processingItemName == null) {
                        processingItemName = formName[0];
                        repositoryValues.put(formName[1], entryArr[1]);

                    // this item belongs to the same entry as the one before
                    } else if (processingItemName.equals(formName[0])) {
                        repositoryValues.put(formName[1], entryArr[1]);

                    // new item, does not belong to the one before
                    } else {
                        //checks if the one before was also the same type
                        if (processingItemName.startsWith("Repository")) {
                            // adds the colected repository values to WPSConfig
                            createRepository(repositoryValues);
                            repositoryValues.clear();
                        }
                        processingItemName = formName[0];
                        repositoryValues.put(formName[1], entryArr[1]);
                    }
                    break;

                case Parser:
                    // splits the formname to the type with number
                    // as identifier
                    formName = entryArr[0].split("_", 2);

                    // the first item to proceed
                    if (processingItemName == null) {
                        processingItemName = formName[0];
                        parserValues.put(formName[1], entryArr[1]);

                    // this item belongs to the same entry as the one before
                    } else if (processingItemName.equals(formName[0])) {
                        parserValues.put(formName[1], entryArr[1]);

                    // new item, does not belong to the one before
                    } else {
                        //checks if the one before was also the same type
                        if (processingItemName.startsWith("Parser")) {
                            // adds the colected parser values to WPSConfig
                            createParser(parserValues);
                            parserValues.clear();
                        }
                        processingItemName = formName[0];
                        parserValues.put(formName[1], entryArr[1]);
                    }
                    break;
                case Generator:
                    // splits the formname to the type with number
                    // as identifier
                    formName = entryArr[0].split("_", 2);

                    // the first item to proceed
                    if (processingItemName == null) {
                        processingItemName = formName[0];
                        generatorValues.put(formName[1], entryArr[1]);

                    // this item belongs to the same entry as the one before
                    } else if (processingItemName.equals(formName[0])) {
                        generatorValues.put(formName[1], entryArr[1]);

                    // new item, does not belong to the one before
                    } else {
                        //checks if the one before was also the same type
                        if (processingItemName.startsWith("Generator")) {
                            // adds the colected generator values to WPSConfig
                            createGenerator(generatorValues);
                            generatorValues.clear();
                        }
                        processingItemName = formName[0];
                        generatorValues.put(formName[1], entryArr[1]);
                    }
                    break;
                               	
            }


        }
        //adds the server values to the WPSConfig
        createServer(serverValues);

        //adds the last not yet added repository to the WPSConfig
        if (!repositoryValues.isEmpty()) {
            createRepository(repositoryValues);
        }

        //adds the last not yet added parser to the WPSConfig
        if (!parserValues.isEmpty()) {
            createParser(parserValues);
        }
        //adds the last not yet added generator to the WPSConfig
        if (!generatorValues.isEmpty()) {
            createGenerator(generatorValues);
        }

        // writes the new WPSConfig to a file
        try {
            String configurationPath = WPSConfig.getConfigPath();
            File XMLFile = new File(configurationPath);
            wpsConfigurationDocument.save(XMLFile, new org.apache.xmlbeans.XmlOptions().setUseDefaultNamespace().setSavePrettyPrint());
            WPSConfig.forceInitialization(configurationPath);
        } catch (IOException e) {
            LOGGER.error("Could not write configuration to file: "+ e.getMessage());
        } catch (org.apache.xmlbeans.XmlException e){
            LOGGER.error("Could not generate XML File from Data: " + e.getMessage());
        }
    }

    /**
     * adds the name value pairs to the Server instance of the WPSconfig
     * @param serverEntries map vith the name value pairs
     */
    private void createServer(HashMap<String, String> serverEntries) {

        if (serverEntries.containsKey("hostname")) {
            server.setHostname(serverEntries.get("hostname"));
        }
        if (serverEntries.containsKey("hostport")) {
            server.setHostport(serverEntries.get("hostport"));
        }
        if (serverEntries.containsKey("includeDataInputsInResponse")) {
            server.setIncludeDataInputsInResponse(Boolean.valueOf(serverEntries.get("includeDataInputsInResponse")));
        }
        if (serverEntries.containsKey("computationTimeoutMilliSeconds")) {
            server.setComputationTimeoutMilliSeconds(serverEntries.get("computationTimeoutMilliSeconds"));
        }
        if (serverEntries.containsKey("cacheCapabilites")) {
            server.setCacheCapabilites(Boolean.valueOf(serverEntries.get("cacheCapabilites")));
        }
        if (serverEntries.containsKey("webappPath")) {
            server.setWebappPath(serverEntries.get("webappPath"));
        }
    }

    /**
     * adds the name value pairs to the RepositoryList instance of the WPSconfig
     * @param repositoryEntries map with the name value pairs belonging to one Repository
     */
    private void createRepository(HashMap<String, String> repositoryEntries) {
        // if Name or Class does not exist, Repository is not added
        if (repositoryEntries.isEmpty() || !repositoryEntries.containsKey("Name") || !repositoryEntries.containsKey("Class")) {
            return;
        }
        // get new repository and add Name and Class
        Repository repository = repositoryList.addNewRepository();
        repository.setName(repositoryEntries.remove("Name"));
        repository.setClassName(repositoryEntries.remove("Class"));

        // if the map has more entries, Properties are present and will be proceed
        if (!repositoryEntries.isEmpty()) {
            repository.setPropertyArray(getPropertyArray(repositoryEntries));
        }
    }

    /**
     * adds the name value pairs to the ParserList instance of the WPSconfig
     * @param parserEntries map with the name value pairs belonging to one Parser
     */
    private void createParser(HashMap<String, String> parserEntries) {
        // if Name or Class does not exist, Parser is not added
        if (parserEntries.isEmpty() || !parserEntries.containsKey("Name") || !parserEntries.containsKey("Class")) {
            return;
        }

        // get new parser and add Name and Class
        Parser parser = parserList.addNewParser();
        parser.setName(parserEntries.remove("Name"));
        parser.setClassName(parserEntries.remove("Class"));

        // if the map has more entries, Properties are present and will be proceed
        if (!parserEntries.isEmpty()) {
            parser.setPropertyArray(getPropertyArray(parserEntries));
        }
    }

    /**
     * adds the name value pairs to the GeneratorList instance of the WPSconfig
     * @param generatorEntries map with the name value pairs belonging to one Generator
     */
    private void createGenerator(HashMap<String, String> generatorEntries) {
        // if Name or Class does not exist, Generator is not added
        if (generatorEntries.isEmpty() || !generatorEntries.containsKey("Name") || !generatorEntries.containsKey("Class")) {
            return;
        }

        // get new generator and add Name and Class
        Generator generator = generatorList.addNewGenerator();
        generator.setName(generatorEntries.remove("Name"));
        generator.setClassName(generatorEntries.remove("Class"));

        // if the map has more entries, Properties are present and will be proceed
        if (!generatorEntries.isEmpty()) {
            generator.setPropertyArray(getPropertyArray(generatorEntries));
        }
    }

    /**
     * geneartes a Property[] of the name value pairs in the map
     * @param properties map with name value pairs belonging to one entry
     * @return Property[]
     */
    private Property[] getPropertyArray(HashMap<String, String> properties) {
        ArrayList<Property> propArr = new ArrayList<Property>();
        while (properties.keySet().iterator().hasNext()) {

            String processingProperty = properties.keySet().iterator().next().split("_")[0];
            String propertyName = properties.remove(processingProperty + "_Name");
            propertyName = URLDecoder.decode(propertyName);
            String propertyValue = properties.remove(processingProperty + "_Value");
            propertyValue = URLDecoder.decode(propertyValue);
            if (propertyName != null) {
                Property prop = Property.Factory.newInstance();
                prop.setName(propertyName);
                if (propertyValue != null) {
                    prop.setStringValue(propertyValue);
                }
                propArr.add(prop);
            }
        }

        Property[] arr = {};
        return propArr.toArray(arr);
    }
}