SLAMon jBPM WorkItemHandler
===========================

[![License][license]](http://www.apache.org/licenses/LICENSE-2.0)
[![Build Status][build]](https://travis-ci.org/SLAMon/slamon-jbpm.svg?branch=master)

#### Table of Contents

* [Building](#building)
* [Installation](#installation)
* [Usage](#usage)
* [Docker images](#docker-images)


This module provides the SLAMon integration to *JBoss Business Process Management Suite* (jBPM) to be used as the tool for
defining and running the actual test processes. jBPM provides tools to graphically describe the test/monitoring
process flows as business processes and this module provides a work item handler in jBPM for distributing the tasks
to agents through the [AFM](https://github.com/SLAMon/slamon-agent-fleet-manager).

A work item handler is a Java class responsible for handling work items in jBPM.
SLAMon provides two jBPM custom work item handlers:

  * **SLAMonWorkItemHandler** or executing SLAMon task handlers
  * **SLAMonJupsHandler** for sending push notifications via JBoss Unified Push Server


Building
--------

The module can be built with the following gradle command:

```bash
    $ gradle jar
```

this will output a *slamon-jbpm-VERSION.jar* in the `build/libs` directory. For convenience, there is an extra target
 `getDepsCompile` to copy all dependencies in the same output directory.

```bash
    $ gradle getDepsCompile
```

Installation
------------

When creating custom work items, you need to create both a Work Item Definition and its corresponding Work Item Handler.
This document only contains brief descriptions of the minimal required steps to get it working, for more information
please refer to the offical jBPM documentation [Chapter 21. Domain-specific Processes](http://docs.jboss.org/jbpm/v6.2/userguide/jBPMDomainSpecificProcesses.html)

### Install the slamon-jbpm module

There are multiple options how to deploy/install the module in the jBPM,
here are few of them:

##### 1. Upload the generated SLAMon jbpm jar package to jBPM via Business Central

This option will require project level settings to include slamon-jbpm in the project deployment.

1. Click Authoring -> Artifact repository
2. In the Artifact repository page, click Upload and provide artifact information
3. Add the SLAMon jbpm jar package to project dependencies
    1. Click Authoring -> Project Authoring
    2. In the Project Authoring page, click Tools -> Project Editor then select Dependencies
    3. Click add from repository then select the SLAMon jbpm jar artifact that was uploaded earlier
4. Add the dependencies of the SLAMon jbpm module to project dependencies
    * Add the following Maven Group ID - Artifact ID - Version ID:
        * com.google.http-client - google-http-client - 1.19.0
        * com.google.http-client - google-http-client-gson - 1.19.0
        * com.google.code.gson - gson - 2.3.1

##### 2. Include in jBPM classpath

It's possible to inject jar package and dependencies in jBPM classpath,
e.g. `$JBOSS_HOME/standalone/deployments/jbpm-console.war/WEB-INF/lib/`.
In this case no dependency definitions on the project level are required.


Usage
-----

### Register work item handlers in the project

To enable SLAMon work item handlers, these need to be declared in the process deployment. Again, there are multiple
ways to do this as jBPB has been evolving. For jBPM 6.2.0 the recommended and easiest way is to define these
in the project deployment descriptor:

1. Click Authoring -> Project Authoring
2. Activate desired project or create a new one
3. Click _Open Project Editor_
4. Scroll down to _Work Item handlers_ section
5. Click the `+ ADD` button
6. Type in the handler name in the first column (e.g. _SLAMon_http_)
7. Type in handler instantiation line (e.g. `new org.slamon.SLAMonWorkItemHandler("http://slamon-afm:8080", "url_http_status", 1)`)
8. Select _Resolver type_: _mvel_
9. Save changes by clicking Save

##### Values for SLAMon handlers

Note that you are free to select the name that suits you, but the name has to
match those in the work item definitions in order to jBPM to do the mapping.

Name        | Value
----------- | -----------------------------------
SLAMon_push | `new org.slamon.SLAMonJupsHandler(JBOSS_UNIFIED_PUSH_SERVER_URL, APPLICATION_ID, MASTER_SECRET, VARIANT_ID)`
SLAMon      | `new org.slamon.SLAMonWorkItemHandler(AGENT_FLEET_MANAGER_URL)`  generic SLAMon task with task type and version specified in the process
SLAMon_task | `new org.slamon.SLAMonWorkItemHandler(AGENT_FLEET_MANAGER_URL, TASK_HANDLER_NAME, TASK_HANDLER_VERSION)` handler with task type and version already bound for convenience

###### Parameters for SLAMonJupsHandler:

Parameter      | Description
-------------- | --------------------------------
JBOSS_UNIFIED_PUSH_SERVER_URL | the JBoss Unified Push Server that is to be used
APPLICATION_ID | the user name to the JBoss Unified Push Server
MASTER_SECRET  | the password to the JBoss Unified Push Server
VARIANT_ID     | the variant ID of the devices where SLAMon will send a copy of every notification (e.g. variant ID used by the service operation center)

###### Parameters for SLAMonWorkItemHandler:

Parameter            | Description
-------------------- | --------------------------------
AGENT_FLEET_MANAGER_URL | the SLAMon Agent Fleet Manager that is to be used
TASK_HANDLER_NAME    | the SLAMon task type that is to be included to the jBPM project
TASK_HANDLER_VERSION | the SLAMon task type version that is to be used
TASK_NAME            | the jBPM custom work item name assigned to the SLAMon task type (Note: this should match the work item definition)


### Work Item Definition

Work item definitions make the items known by jBPM and visible in the business process designer.
Quick steps to create a work item definition in Business Central:

1. Click Authoring -> Project Authoring
2. In the Project Authoring page, click New Item -> Work Item definition
3. Add the entries for SLAMon.

Use the following for SLAMonJupsHandler:

    import org.drools.core.process.core.datatype.impl.type.StringDataType;
    import org.drools.core.process.core.datatype.impl.type.IntegerDataType;

    [
        [
            "name" : "SLAMon_push",
            "parameters" : [
                "variant_id" : new StringDataType(),
                "alert" : new StringDataType(),
                "sound" : new StringDataType()
            ],
            "results" : [
                "result" : new StringDataType()
            ],
            "category": "SLAMon",
            "displayName" : "Push Notification",
            "icon" : "defaultservicenodeicon.png"
        ]
    ]


Please refer to the Usage section of this readme for the description of the parameters and results.

You can use the following as template for each SLAMonWorkItemHandler to be used:

    import org.drools.core.process.core.datatype.impl.type.IntegerDataType;

    [
        [
            "name" : "TASK_NAME",
            "parameters" : [
                "INPUT_PARAMETER1" : new StringDataType(),
                "INPUT_PARAMETER2" : new IntegerDataType()
            ],
            "results" : [
                "OUTPUT_PARAMETER1" : new StringDataType(),
                "OUTPUT_PARAMETER2" : new IntegerDataType()
            ],
            "category": "SLAMon",
            "displayName" : "DISPLAY_NAME",
            "icon" : "defaultservicenodeicon.png"
        ]
    ]

Where:

  * TASK_NAME - the jBPM custom work item name assigned to the SLAMon task type
  * INPUT_PARAMETER - the input parameters of the SLAMon task type
  * OUTPUT_PARAMETER - the output parameters of the SLAMon task type
  * DISPLAY_NAME - the name that will appear in the Process Designer Canvas Object Library palette in Business Central

Please refer to the readme of the corresponding SLAMon task type that you are going to use for the actual parameter names and descriptions.

### Using SLAMon work items in business process

By default, custom work items are located under _Service Tasks_ of the
_Process Designer Canvas Object Library_ palette in _Business Central_.
This can be overridden with _category_ definition in the work item definitions.

#### SLAMonWorkItemHandler

Each SLAMon task type is represented by a separate custom work item in jBPM.

The DataInput of each custom work item corresponds to the input data parameter of the equivalent SLAMon task handler.

Likewise, the DataOutput of each custom work item corresponds to the response data return by the equivalent SLAMon task handler.

Please refer the readme files of the SLAMon Agent and the corresponding task types that you are going to use for more details.


#### SLAMonJupsHandler

The SLAMon Push Notification custom work item can be used in a test process to send a notification to a mobile device.

SLAMon Push Notification accepts the following DataInput:

  * variant_id - the id of the group of devices where the notification will be sent
  * alert - the message to be sent
  * sound - the sound that will be played on the receiving devices

Take note that a copy of the notification will always be sent to the variant id defined in the Work Item Handler registration. For example, this can be the variant ID used by the service operation center.

A notification can also be sent to all devices. To do this, use "all" instead of specifying a specific variant_id.

If you do not have a specific sound to play, please use "default".

SLAMon Push Notification returns the following DataOutput:

  * result - either "successful" or "failed"


Docker images
-------------

Pre-existing images are built from `master` and `dev` branches:
`slamon/jbpm:stable` and `slamon/jbpm:latest` respectively, and also
from GitHub tags as `slamon/jbpm:<tag>`.

The images are extending [jboss/jbpm-workbench](https://hub.docker.com/r/jboss/jbpm-workbench/)
with *slamon-jbpm* and dependencies included in the jBPM classpath and some
sample configurations as defaults or templates to automatically include SLAMon
work item handlers and work item definitions in newly created projects.

### Using the images

Included template configs are predefined to search for [AFM](https://github.com/SLAMon/slamon-agent-fleet-manager)
at `http://slamon-afm:8080`. To alter this, you need to override these default
values.

#### jBPM+Agent+jBPM stack with Docker compose

The following snippet can be used, combining jBPM, an agent and the AFM:

```yml
afm:
  image: slamon/afm:stable
  ports:
   - "8081:8080"
jbpm:
  image: slamon/jbpm:stable
  links:
   - afm:slamon-afm
  ports:
   - "8080:8080"
agent:
  image: slamon/agent:stable
  environment:
    AFM: "http://slamon-afm:8080"
    HANDLERS: "slamon_agent.handlers"
    EXTRA_FLAGS: "-v"
  links:
   - afm:slamon-afm
```

This will bind AFM to host port 8081 and jBPM  to 8080.

* AFM dashboard: http://localhost:8081/dashboard/
* jBPM Workbench: http://localhost:8081/jbpm-console/
* JBoss Dashbuilder: http://localhost:8080/dashbuilder/

[license]: https://img.shields.io/:license-Apache%20License%20v2.0-blue.svg
[build]: https://travis-ci.org/SLAMon/slamon-jbpm.svg?branch=master
