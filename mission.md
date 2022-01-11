## Mission
Data management systems serve two purposes: a) storing and manipulating persistent data and b) querying and analyzing it. While the former is highly sensitive to size, shape and further characteristics of the actual data, the latter embodies an aspect commonly present across all types of data management systems: primitives to transform, filter, combine and aggregate large amounts of data.

Despite numerous research efforts in the field of query algebras and generalized query compilers, this commonality is still not exploited in a widely-applicable compilation and evaluation infrastructure. One factor might be the wish to tailor the compilation process for a specialized data storage and workload, another one might be the undisputed complexity of getting a decently running system out of the elaborate formalisms. Fact is that most systems are still implemented from scratch, although it costs enormous effort to re-implement query algorithms and to transfer the experience of decades in query compilation and optimization to each new system.

In the field of semi-structured data, we naturally have less chances – and sometimes perhaps less pressure – to squeeze the last bit of performance: we rarely have clear-cut workload patterns, input data may come from various internal and external sources (native storage, files, network, key-value stores), and we may have to copy with deeply nested, irregular data of (partially) unknown shape. In face of these challenges, we argue that every piece of proven optimization techniques and evaluation algorithms one can rely on is already a win.

We draw additional motivation from the observation that the data management landscape is currently in a phase of substantial change, which was and still is fueled by the following factors:

- an increasing share of data is semi-structured or heterogenous (e.g., XML, JSON)
- a trend towards more complex and scripting-style data management and analysis processes
- the need to embrace parallel and distributed system architectures (Mulit-core, Many-core, Cloud)
- dynamic environments demand for ad-hoc data analysis – often with (near-)realtime requirements
- the ongoing growth of data which still does not show any sign of a slow down

Both, research and industry reacted to these challenges with a broad range of powerful designs and paradigms, e.g., XML databases, document databases, key-value stores, MapReduce etc. As a result, we observe a now growing diversification in the market of data management software.

In sum, the variety and freedom of choice is not only a blessing – for both, customers and DBMS vendors. Application developers need to pick one or even several products and orchestrate the various APIs and programming paradigms. Vendors must face the fact that bare performance of a particular data storage is not sufficient to build a successful DBMS. It requires a comfortable, powerful programming abstraction – ideally a query or scripting language – to allow developers to achieve their goals without having to implement low-level, performance-critical operations manually.

With our research, we want to develop an extensible, retargetable compilation and runtime framework for driving scripting-like query languages for structured and semi-structured data. Ideally, we envision to prepare the ground for a tool for query languages similar to what the llvm compiler framework is for general programming languages: an ecosystem in which proven optimization techniques and algorithms are readily available for implementing custom query processing systems.

Our work is guided by the following basic questions:

- What is an appropriate level of abstraction for accessing and (de-)composing semi-structured data? How should we represent semi-structured data with deep nestings and varying sizes at runtime? How can we support native operations and alternative access methods, i.e., indexes etc.?
- How should we represent queries, whole scripts, and user-defined functions to leverage proven optimization techniques from both query languages (e.g., predicate-pushdown, join ordering, optimized aggregation) and programming languages (e.g., tail recursion)?
- How can we support different evaluation philosophies (pull-based, push-based, mixed push/pull) and proven query algorithms?. How can we (automatically) exploit partitioned data, parallel hardware and query-inherent parallelism?
- How can we incorporate physical and system-specific properties (e.g., memory locality) in the compilation and evaluation process?
