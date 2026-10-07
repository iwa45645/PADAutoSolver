package com.example.padautosolver;

/** Maximum HP is observed, never a team constant. Two fresh agreeing reads are required. */
final class UraHpEvidence {
    private int candidate,maximum;
    private long sequence=-1,observedAt=-1;
    void observe(int[] hp,long time,long seq) {
        if(seq<=sequence)return;
        sequence=seq;
        if(hp==null){candidate=maximum=0;observedAt=-1;return;}
        if(time==observedAt)return;
        if(hp.length!=2||hp[1]<100000||hp[0]<0||hp[0]>hp[1]){candidate=maximum=0;observedAt=-1;return;}
        if(time<observedAt||time-observedAt>15000){candidate=maximum=0;}
        if(candidate!=hp[1]){candidate=hp[1];maximum=0;}
        else maximum=candidate;
        observedAt=time;
    }
    int maximum(long time) {
        return observedAt>=0&&time>=observedAt&&time-observedAt<=15000?maximum:0;
    }
    boolean matches(int[] hp,long time) {
        return hp!=null&&hp.length==2&&maximum(time)>0&&hp[1]==maximum(time)&&hp[0]>=0&&hp[0]<=hp[1];
    }
}
