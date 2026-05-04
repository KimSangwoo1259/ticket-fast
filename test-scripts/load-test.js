import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { check, sleep } from 'k6';
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';

// 1. CSV 데이터 로드 (50만 개 TSID)
const seatData = new SharedArray('seat_ids', function () {
    return papaparse.parse(open('./seats.csv'), { header: true }).data;
});

export const options = {
    vus: 100,          // 100명의 가상 유저
    duration: '30s',   // 30초 동안 테스트
};

export default function () {
    // 2. 앞에 100개로 대상 제한
    const randomIndex = Math.floor(Math.random() * 100);
    const seatId = seatData[randomIndex].id; // CSV 헤더가 id인 경우

    const url = 'http://3.39.123.195:8080/api/reservation';

    const payload = JSON.stringify({
        performanceSeatId: seatId
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-USER-ID': String(__VU), // ArgumentResolver를 위한 헤더
            'X-TEST-MODE': 'true',
            'X-USER-ROLE': 'USER'
        },
    };

    // 3. 요청 전송
    const res = http.post(url, payload, params);

    // 에러가 날 때 딱 한 번만 로그를 찍어봅니다.
    if (res.status !== 201 && res.status !== 409) {
        console.log(`에러 발생! 상태코드: ${res.status}, 응답내용: ${res.body}`);
    }

    // 4. 결과 검증
    check(res, {
        // 성공 시 201(Created), 이미 선점된 경우 409(Conflict) 혹은 상우님이 설정한 예외 코드
        '성공(201) 혹은 선점됨(409)': (r) => r.status === 201 || r.status === 409,
        '응답 시간 < 200ms': (r) => r.timings.duration < 200,
    });

    // 0.1초 대기 (광클 시뮬레이션)
    sleep(0.1);
}