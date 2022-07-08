package com.thanhtu.crud.controller.admin;

import com.thanhtu.crud.exception.NotFoundException;
import com.thanhtu.crud.model.dto.BestSellingProductsPage;
import com.thanhtu.crud.model.dto.GeneralStatiscts;
import com.thanhtu.crud.model.request.RequestDate;
import com.thanhtu.crud.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
//@PreAuthorize("hasAuthority('ADMIN')")
@CrossOrigin(origins = "https://react-admin-eight.vercel.app")
@RequestMapping("admin/statistic")
public class StatisticController {
    @Autowired
    StatisticService statisticService;

    @GetMapping("/general")
    public ResponseEntity<?> generalStatistict()
    {
        GeneralStatiscts generalStatiscts=statisticService.generalStatistict();
        return ResponseEntity.status(HttpStatus.OK).body(generalStatiscts);
    }

    @PostMapping("/revenue")
    public ResponseEntity<?> revenueStatistics(@RequestBody RequestDate requestDate)
    {
        long sumRevenue=statisticService.revenueStatistics(requestDate);
        return ResponseEntity.status(HttpStatus.OK).body(sumRevenue);
    }
    @PostMapping("/bestsellingproducts")
    public ResponseEntity<?> bestSellingProducts(@RequestBody RequestDate requestDate,
                                                 @RequestParam(value = "page",required = false) Integer page)
    {
        if(page==null)
        {
            page=0;
        }
        else{
            page=page-1;
        }
        BestSellingProductsPage list=statisticService.bestSellingProducts(requestDate,page);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }
}
