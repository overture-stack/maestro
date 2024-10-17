# What is Maestro

Maestro is a micro service that listens to changes on any type of data and mantains a ElasticSearch index updated.

Data changes can be delivered via various sources, including an HTTP API, a JavaScript package, or Kafka, having the possibility to connect to a genomic metadata server [SONG](https://www.overture.bio/products/song) and listens to changes there to build an Elasticsearch index.

Maestro was created to enable genomic researchers to enhance their Overture metadata storage system[SONG](https://www.overture.bio/products/song) by building search indexes, Elasticsearch by default, that makes searching Analyses and Studies easy and powerful.

On the other end, Indexes created my Maestro can be consumed [Arranger](https://www.overture.bio/products/arranger), a data portal generator.

# License

Copyright (c) 2024. Ontario Institute for Cancer Research

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see https://www.gnu.org/licenses.
