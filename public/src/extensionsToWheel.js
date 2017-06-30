
function WheelLayout() {
    go.CircularLayout.call(this);
}
go.Diagram.inherit(WheelLayout, go.CircularLayout);

// override makeNetwork to set the diameter of each node and ignore the TextBlock label
/** @override */
WheelLayout.prototype.makeNetwork = function(collection) {
    var layoutNetwork = go.CircularLayout.prototype.makeNetwork.call(this, collection);
    layoutNetwork.vertexes.each(function(layoutVertex) {
        layoutVertex.diameter = 20;
        // because our desiredSize for nodes is (20, 20)
    });
    return layoutNetwork;
}

// override commitNodes to rotate nodes so the text goes away from the center,
// and flip text if it would be upside-down
/** @override */
WheelLayout.prototype.commitNodes = function() {
    go.CircularLayout.prototype.commitNodes.call(this);
    this.network.vertexes.each(function(vertex) {
        var node = vertex.node;
        if (node === null )
            return;
        // get the angle of the node towards the center, and rotate it accordingly
        var angle = vertex.actualAngle;
        if (angle > 90 && angle < 270) {
            // make sure the text isn't upside down
            var textBlock = node.findObject("TEXTBLOCK");
            textBlock.angle = 180;
        }
        node.angle = angle;
    });
};
// end WheelLayout class


function makeDiagram(element) {
    var $ = go.GraphObject.make;
    // for conciseness in defining templates

    var highlightColor = "red";
    // color parameterization
    
    var myDiagram = $(go.Diagram, element, // must be the ID or reference to div
    {
        initialAutoScale: go.Diagram.Uniform,
        padding: 20,
        contentAlignment: go.Spot.Center,
        layout: $(WheelLayout, // set up a custom CircularLayout
        // set some properties appropriate for this sample
        {
            arrangement: go.CircularLayout.ConstantDistance,
            nodeDiameterFormula: go.CircularLayout.Circular,
            spacing: 40,
            aspectRatio: 0.4,
            sorting: go.CircularLayout.Ascending
        }),
        isReadOnly: true,
        click: function(e) {
            // background click clears any remaining highlighteds
            e.diagram.startTransaction("clear");
            e.diagram.clearHighlighteds();
            e.diagram.commitTransaction("clear");
        }
    });

    // define the Node template
    myDiagram.nodeTemplate = $(go.Node, 
    "Horizontal",
    {
        selectionAdorned: false,
        locationSpot: go.Spot.Center,
        // Node.location is the center of the Shape
        locationObjectName: "SHAPE",
        mouseEnter: function(e, node) {
            node.diagram.clearHighlighteds();
            node.linksConnected.each(function(link) {
                highlightLink(link, true);
            });
            node.isHighlighted = true;
            var textBlock = node.findObject("TEXTBLOCK");
            if (textBlock !== null )
                textBlock.stroke = highlightColor;
        },
        mouseLeave: function(e, node) {
            node.diagram.clearHighlighteds();
            var textBlock = node.findObject("TEXTBLOCK");
            if (textBlock !== null )
               textBlock.stroke = "black";
        }
    },
    new go.Binding("text","text"), // for sorting the nodes
    $(go.Shape, 
        "Ellipse",
        {
            name: "SHAPE",
            fill: "lightgray",
            // default value, but also data-bound
            stroke: "transparent",
            // modified by highlighting
            strokeWidth: 2,
            desiredSize: new go.Size(20,20),
            portId: ""
        }, // so links will go to the shape, not the whole node
        new go.Binding("fill","color"),
        new go.Binding("stroke","isHighlighted",
            function(h) {
                return h ? highlightColor : "transparent";
            }
        ).ofObject()
    ),
    $(go.TextBlock,
        {
            name: "TEXTBLOCK"
        }, // for search
        new go.Binding("text","text")
    )
    );  // end myDiagram's nodeTemplate

    function highlightLink(link, show) {
        link.isHighlighted = show;
        link.fromNode.isHighlighted = show;
        link.toNode.isHighlighted = show;
    }

    // define the Link template
    myDiagram.linkTemplate = $(go.Link,
    {
        routing: go.Link.Normal,
        curve: go.Link.Bezier,
        selectionAdorned: false,
        mouseEnter: function(e, link) {
            highlightLink(link, true);
        },
        mouseLeave: function(e, link) {
            highlightLink(link, false);
        }
    },
    $(go.Shape,
        new go.Binding(
            "stroke",
            "isHighlighted",
            function(h, shape) {
                return h ? highlightColor : shape.part.data.color;
            }
        ).ofObject(),
        new go.Binding(
            "strokeWidth",
            "isHighlighted",
            function(h) {
                return h ? 2 : 1;
            }
        ).ofObject()
    ),
    $(go.Shape, { toArrow: "Standard" })
    );  // end myDiagram's linkTemplate

    return myDiagram;
} // end makeDiagram

function makeModel() {

    return Promise.all([get('/data/nodeData.json'), get('/data/linkData.json')])
    .then(function(serverFiles) {
        var data = {
            nodeDataArray: JSON.parse(serverFiles[0]),
            linkDataArray: JSON.parse(serverFiles[1])
        };
        return model = new go.GraphLinksModel(data.nodeDataArray,
                                              data.linkDataArray);
    }).catch(function(error) {
        console.log("Failed!", error);
    });
}

function get(url) {
    return new Promise(function(resolve, reject) {
        var req = new XMLHttpRequest();
        req.open('GET', url);
        req.onload = function() {
            if (req.status == 200) {
                resolve(req.response);
            } else {
                reject(Error(req.statusText));
            }
        };
        req.onerror = function() {
            reject(Error("No file at URL: " + url));
        };
        req.send();
    });
}

function init() {
    var myDiagram = makeDiagram("myDiagramDiv");
    makeModel().then(function(model) {
        myDiagram.model = model;
    });
    
    get('/data/result.txt')
    .then(function(resp) {
        document.getElementById("resultDiv").innerHTML = resp;
    }).catch(function(error) {
        console.log("Failed!", error);
    });
}


