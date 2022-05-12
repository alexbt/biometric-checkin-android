package com.atharvakale.facerecognition;

class PreferenceMember {
    private Member member;
    private SimilarityClassifier.Recognition recognition;

    public PreferenceMember(Member member, SimilarityClassifier.Recognition recognition) {
        this.member = member;
        this.recognition = recognition;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public SimilarityClassifier.Recognition getRecognition() {
        return recognition;
    }

    public void setRecognition(SimilarityClassifier.Recognition recognition) {
        this.recognition = recognition;
    }
}
