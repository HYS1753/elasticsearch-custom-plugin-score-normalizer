# Elasticsearch Custom plugin<br/>(Score normalizer with query rescorer)

--- 

## Overview
이 프로젝트는 Elasticsearch 검색 결과의 점수를 normalize할 수 있는 query rescorer를 제공합니다.</br>
이 rescorer는 다양한 사용 사례와 데이터 분포에 맞춘 여러 정규화 기법을 지원하여,</br>
보다 일관되고 의미 있는 점수 산출을 가능하게 합니다.</br>
사용 가능한 정규화 방법은 다음과 같습니다:

**Min-Max 정규화**: <br/>
점수를 지정된 범위(일반적으로 [0, 1])로 스케일링하여, 결과의 상대적 순서를 유지합니다.

> $\text{Normalized Score} = \frac{\text{Original Score} - \text{Min Score}}{\text{Max Score} - \text{Min Score}}$ <br/><br/>
> *Original Score*: Elasticsaerch 기본 검색 결과 score<br/>
> *Min Score*: 각 샤드의 검색 결과 중 window size 내 score 최솟값<br/>
> *Max Score*: 각 샤드의 검색 결과 중 window size 내 score 최댓값

**Z-Score 정규화**: <br/>평균에서 표준편차 단위로 점수를 표준화하여, 정규 분포된 데이터에 적합한 정규화 방법입니다.

> $\text{Z-Score} = \frac{\text{Original Score} - \mu}{\sigma}$ <br/><br/>
> $\mu$: 각 샤드의 검색 결과 중 window size 내 전체 score 평균 <br/>
> $\sigma$: 각 샤드의 검색 결과 중 window size 내 전체 score 표준편차 <br/>
> &nbsp;&nbsp;&nbsp; (표준편차 계산식: σ = √(Σ((xi - μ)^2) / N) [xi = 각 데이터 값, μ = 모집단 평균, N = 데이터 값의 개수] )

**Robust 정규화**: <br/>사분위 범위를 기반으로 정규화하여, 극단값이 있는 데이터셋에서도 안정적인 점수 산출이 가능합니다.

> $\text{Normalized Score} = \frac{\text{Original Score} - Q1}{Q3 - Q1}$

## Tech Stack
- JAVA 17
- Gradle 8.6
- org.elasticsearch.elasticsearch 8.12.2
- org.junit.jupiter.junit-jupiter-api 5.7.0
- org.junit.jupiter.junit-jupiter-engine 5.7.0
- docker image
  - docker.elastic.co/elasticsearch/elasticsearch:8.12.2
  - docker.elastic.co/kibana/kibana:8.12.2

## Prerequisites
해당 플러그인은 Elasticsearch 8.12.2 버전에 호환됩니다. <br/>
docker를 통한 테스트 환경 구성을 할 수 있습니다. <br/>
(root 디렉터리 기준 `./docker/start-es-docker.sh` 실행)

## Usage
### Options
> **window_size** : <br/>
> &nbsp;&nbsp;&nbsp;&nbsp;rescoring 할 대상 문서 수<br/>
> **normalizer_type** : <br/>
> &nbsp;&nbsp;&nbsp;&nbsp;정규화 알고리즘 (min_max, z_score, robust)<br/>
> **factor** : <br/>
> &nbsp;&nbsp;&nbsp;&nbsp;정규화 된 점수의 factor<br/>
> **factor_mode** : <br/>
> &nbsp;&nbsp;&nbsp;&nbsp;위의 factor 적용 모드 (sum, multiply, increase_by_percent)<br/>
> **min_score, max_score** : <br/>
> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(min_max 알고리즘 한정) 정규화된 점수 사용자 지정 Min,Max score 보정<br/>

### Min-Max
```
GET index_name/_search
{
  "query": {
    ...
  },
  "rescore": {
    "window_size": 50,
    "score_normalizer": {
      "normalizer_type": "min-max",
      "factor": 1,
      "factor_mode": "multiply",
      "min_score": 0,
      "max_score": 1
    }
  }
}
```
### Z-Score

```
GET index_name/_search
{
  "query": {
    ...
  },
  "rescore": {
    "window_size": 50,
    "score_normalizer": {
      "normalizer_type": "z_score",
      "factor": 1,
      "factor_mode": "multiply"
    }
  }
}
```
### Robust
```
GET index_name/_search
{
  "query": {
    ...
  },
  "rescore": {
    "window_size": 50,
    "score_normalizer": {
      "normalizer_type": "robust",
      "factor": 1,
      "factor_mode": "multiply"
    }
  }
}
```

## Example
### score 보정 전
![score보정전](https://github.com/user-attachments/assets/1c9ba790-c767-47b9-9cab-7883877efa3b)
### score 보정 후(min_max)
![score보정후](https://github.com/user-attachments/assets/2acebb3b-67db-4fec-aa06-7360dfb568cb)