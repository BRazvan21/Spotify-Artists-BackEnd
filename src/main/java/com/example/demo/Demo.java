package com.example.demo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;


@RestController()
@RequestMapping("/demo")
@CrossOrigin(origins = "http://127.0.0.1:5500") // allow your frontend origin
public class Demo {

    private final BazaDeDateRepository repo;

    public Demo(BazaDeDateRepository repo) {
        this.repo = repo;
    }
    @GetMapping("/hello/{nume}")
    public String sayHello(@PathVariable String nume) {
        return new JSONArray(repo.findByNume(nume)).toString();
    }

    @PostMapping("/adauga")
    public String adauga(@RequestBody BazaDeDate baza) throws JSONException {
        BazaDeDate salvata = repo.save(baza);

        JSONObject raspuns = new JSONObject();
        raspuns.put("status", "ok");
        raspuns.put("id", salvata.getId());
        raspuns.put("nume", salvata.getNume());
        raspuns.put("mesaj", "Obiect salvat cu succes in baza de date.");

        return raspuns.toString();
    }

}
