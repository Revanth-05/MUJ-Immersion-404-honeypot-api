package com.scam.honeypot;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.*;

@RestController
public class HoneypotController {

    private final String API_KEY = "mysecret123";
    private final String GUVI_URL =
        "https://hackathon.guvi.in/api/updateHoneyPotFinalResult";

    private final List<String> SCAM_WORDS = Arrays.asList(
        "blocked","verify","urgent","upi","account",
        "suspend","click","bank","link"
    );


    @PostMapping("/honeypot")
    public ResponseEntity<Map<String,Object>> honeypot(
        @RequestBody Map<String,Object> data,
        @RequestHeader("x-api-key") String key
    ){

        if(!API_KEY.equals(key)){
            return ResponseEntity.status(401).build();
        }

        String sessionId = (String) data.get("sessionId");

        Map msg = (Map) data.get("message");
        String text = (String) msg.get("text");

        List history =
            (List) data.getOrDefault("conversationHistory",new ArrayList<>());

        boolean scam = isScam(text);

        Map<String,Object> res = new HashMap<>();

        if(!scam){
            res.put("status","success");
            res.put("reply","Okay, noted.");
            return ResponseEntity.ok(res);
        }

        Map<String,Object> intel = extract(text);

        int total = history.size() + 1;

        if(total >= 6){
            sendFinal(sessionId,total,intel);
        }

        res.put("status","success");
        res.put("reply",humanReply());

        return ResponseEntity.ok(res);
    }


    private boolean isScam(String text){
        text = text.toLowerCase();
        for(String w:SCAM_WORDS){
            if(text.contains(w)) return true;
        }
        return false;
    }


    private Map<String,Object> extract(String text){

        Map<String,Object> map = new HashMap<>();

        map.put("bankAccounts",
            find("\\d{4}-\\d{4}-\\d{4}",text));

        map.put("upiIds",
            find("\\w+@\\w+",text));

        map.put("phishingLinks",
            find("https?://\\S+",text));

        map.put("phoneNumbers",
            find("\\+91\\d{10}",text));

        List<String> keys = new ArrayList<>();

        for(String w:SCAM_WORDS){
            if(text.toLowerCase().contains(w)){
                keys.add(w);
            }
        }

        map.put("suspiciousKeywords",keys);

        return map;
    }


    private List<String> find(String regex,String text){

        List<String> list = new ArrayList<>();

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);

        while(m.find()){
            list.add(m.group());
        }

        return list;
    }


    private void sendFinal(String sessionId,
                           int total,
                           Map<String,Object> intel){

        RestTemplate rt = new RestTemplate();

        Map<String,Object> payload = new HashMap<>();

        payload.put("sessionId",sessionId);
        payload.put("scamDetected",true);
        payload.put("totalMessagesExchanged",total);
        payload.put("extractedIntelligence",intel);
        payload.put("agentNotes","Urgency + verification scam");

        try{
            rt.postForObject(GUVI_URL,payload,String.class);
        }catch(Exception e){}
    }


    private String humanReply(){

        String[] r = {
            "Why is my account blocked?",
            "Is this really from bank?",
            "Please explain properly",
            "I am confused",
            "What should I do now?"
        };

        return r[new Random().nextInt(r.length)];
    }
}
