package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDao;
import com.techelevator.tenmo.dao.TransferDao;
import com.techelevator.tenmo.dao.UserDao;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("isAuthenticated()")
public class AccountController {
    private AccountDao accountDao;
    private TransferDao transferDao;
    private UserDao userDao;
    private static final int APPROVE_TRANSFER = 1;
    private static final int REJECT_TRANSFER = 2;

    public AccountController(AccountDao accountDao, TransferDao transferDao, UserDao userDao) {
        this.accountDao = accountDao;
        this.transferDao = transferDao;
        this.userDao = userDao;
    }

    @RequestMapping(path = "/balance", method = RequestMethod.GET)
    public BigDecimal getBalance(Principal principal) {
        int userId = userDao.findIdByUsername(principal.getName());
        BigDecimal balance = accountDao.getBalance(userId);
        return balance;
    }


    @RequestMapping(path = "/transfers", method = RequestMethod.POST)
    public Transfer sendMoney(Principal principal, @Valid @RequestBody Transfer transfer) {
        int toUserId = transfer.getToUserId();
        int fromUserId = userDao.findIdByUsername(principal.getName());
        transfer.setFromUserId(fromUserId);
        transfer.setAccountFrom(transferDao.getAccountIdByUserId(transfer.getFromUserId()));
        transfer.setAccountTo(transferDao.getAccountIdByUserId(transfer.getToUserId()));
        transfer.setTransferType("Send");
        if(getBalance(principal).subtract(transfer.getAmount()).compareTo(BigDecimal.valueOf(0)) == 0 || getBalance(principal).subtract(transfer.getAmount()).compareTo(BigDecimal.valueOf(0)) == 1) {
            transfer.setTransferStatus("Approved");
            transfer = transferDao.createTransfer(transfer);
            transferDao.approvedTransfer(transfer);
        } else {
            transfer.setTransferStatus("Rejected");
            transfer = transferDao.createTransfer(transfer);
        }
        transfer.setToUserId(toUserId);
        transfer.setFromUserId(fromUserId);
        transfer.setFromUsername(transferDao.getUsernameByUserId(fromUserId));
        transfer.setToUsername(transferDao.getUsernameByUserId(toUserId));

        return transfer;
    }

    @RequestMapping(path = "/transfers/request", method = RequestMethod.POST)
    public Transfer requestMoney(Principal principal, @Valid @RequestBody Transfer transfer) {
        int toUserId = userDao.findIdByUsername(principal.getName());
        int fromUserId = transfer.getFromUserId();
        transfer.setFromUserId(fromUserId);
        transfer.setTransferStatus("Pending");
        transfer.setTransferType("Request");
        transfer.setToUserId(toUserId);
        transfer.setFromUserId(fromUserId);
        transfer.setAccountFrom(transferDao.getAccountIdByUserId(transfer.getFromUserId()));
        transfer.setAccountTo(transferDao.getAccountIdByUserId(transfer.getToUserId()));
        transfer = transferDao.createTransfer(transfer);
        transfer.setFromUsername(transferDao.getUsernameByUserId(fromUserId));
        transfer.setToUsername(transferDao.getUsernameByUserId(toUserId));
        return transfer;
    }

    @RequestMapping(path = "/users", method = RequestMethod.GET)
    public Map<Integer, String> getUsers(Principal principal) {
        Map<Integer, String> users = new HashMap<>();
        List<User> userList = userDao.findAll();
        for (User user: userList) {
            if (user.getId() != userDao.findIdByUsername(principal.getName())) {
                users.put(user.getId().intValue(), user.getUsername());
            }
        }
        return users;
    }

    @RequestMapping(path = "/transfers/completed", method = RequestMethod.GET)
    public List<Transfer> getCompletedTransfers(Principal principal) {
        int userId = userDao.findIdByUsername(principal.getName());
        List<Transfer> approvedTransferList = transferDao.getTransfers(userId, 2);
        List<Transfer> rejectedTransferList = transferDao.getTransfers(userId, 3);
        for (Transfer transfer: rejectedTransferList) {
            approvedTransferList.add(transfer);
        }
        return approvedTransferList;
    }

    @RequestMapping(path = "/transfers/pending", method = RequestMethod.GET)
    public List<Transfer> getPendingTransfers(Principal principal) {
        int userId = userDao.findIdByUsername(principal.getName());
        List<Transfer> transferList = transferDao.getTransfers(userId, 1);
        return transferList;
    }

    @RequestMapping(path = "/transfers/approve", method = RequestMethod.PUT)
    public Transfer approveTransfer(Principal principal, @Valid @RequestBody Transfer transfer) {
        if (transfer.getFromUserId() == userDao.findIdByUsername(principal.getName())) {
            if (getBalance(principal).subtract(transfer.getAmount()).compareTo(BigDecimal.valueOf(0)) == 0 || getBalance(principal).subtract(transfer.getAmount()).compareTo(BigDecimal.valueOf(0)) == 1) {
                Transfer updatedTransfer = new Transfer();
                updatedTransfer = transferDao.updateTransfer(transfer.getTransferId(), 2);
                transferDao.approvedTransfer(transfer);
                return updatedTransfer;
            } else {
                return transfer;
            }
        } else {
            return transfer;
        }
    }

    @RequestMapping(path = "/transfers/reject", method = RequestMethod.PUT)
    public Transfer rejectTransfer(Principal principal, @Valid @RequestBody Transfer transfer) {
        Transfer updatedTransfer = new Transfer();
        updatedTransfer = transferDao.updateTransfer(transfer.getTransferId(), 3);
        return updatedTransfer;

    }
}
