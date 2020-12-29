package io.horizontalsystems.ethereumkit.core

/*class TransactionManager(
        private val storage: ITransactionStorage,
        private val transactionsProvider: ITransactionsProvider
) : ITransactionManager {

    private var delayTime = 3 // in seconds
    private val delayTimeIncreaseFactor = 2
    private var retryCount = 0

    private var disposables = CompositeDisposable()

    override var syncState: SyncState = SyncState.NotSynced(SyncError.NotStarted())
        private set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateTransactionsSyncState(value)
            }
        }

    override val source: String
        get() = transactionsProvider.source

    override var listener: ITransactionManagerListener? = null

    private fun update(transactions: List<EtherscanTransaction>, internalTransactions: List<InternalTransaction>) {
        storage.saveTransactions(transactions)
        storage.saveInternalTransactions(internalTransactions)

        val transactionsWithInternal = transactions.mapNotNull { transaction ->
            val internalTransactionList = internalTransactions.filter { it.hash.contentEquals(transaction.hash) }
            val transactionWithInternal = TransactionWithInternal(transaction, internalTransactionList)

            if (!isEmpty(transactionWithInternal)) {
                transactionWithInternal
            } else {
                null
            }
        }

        listener?.onUpdateTransactions(transactionsWithInternal)
    }

    private fun isEmpty(transactionWithInternal: TransactionWithInternal): Boolean {
        return transactionWithInternal.transaction.value == BigInteger.ZERO && transactionWithInternal.internalTransactions.isEmpty()
    }

    private fun sync(delayTimeInSeconds: Int? = null) {
        val lastTransactionBlockHeight = storage.getLastTransactionBlockHeight() ?: 0
        val lastInternalTransactionBlockHeight = storage.getLastInternalTransactionBlockHeight() ?: 0

        var requestsSingle = Single.zip(
                Single.just(listOf()),
                Single.just(listOf()),
//                transactionsProvider.getTransactions(lastTransactionBlockHeight + 1),
//                transactionsProvider.getInternalTransactions(lastInternalTransactionBlockHeight + 1),
                BiFunction<List<EtherscanTransaction>, List<InternalTransaction>, Pair<List<EtherscanTransaction>, List<InternalTransaction>>> { t1, t2 -> Pair(t1, t2) }
        )

        delayTimeInSeconds?.let {
            requestsSingle = requestsSingle.delaySubscription(delayTimeInSeconds.toLong(), TimeUnit.SECONDS)
        }

        requestsSingle
                .subscribeOn(Schedulers.io())
                .subscribe({ (transactions, internalTransactions) ->
                    if (retryCount > 0 && transactions.isEmpty() && internalTransactions.isEmpty()) {
                        retryCount -= 1
                        delayTime *= delayTimeIncreaseFactor
                        sync(delayTime)
                        return@subscribe
                    }
                    retryCount = 0
                    update(transactions, internalTransactions)
                    syncState = SyncState.Synced()
                }, { error ->
                    retryCount = 0
                    syncState = SyncState.NotSynced(error)
                })
                .let {
                    disposables.add(it)
                }
    }

    override fun refresh(delay: Boolean) {
        if (syncState is SyncState.Syncing) {
            return
        }
        syncState = SyncState.Syncing()

        if (delay) {
            retryCount = 5
            delayTime = 3

            sync(delayTime)
        } else {
            sync()
        }
    }

    override fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<TransactionWithInternal>> {
        return storage.getTransactions(fromHash, limit)
    }

    override fun handle(transaction: EtherscanTransaction) {
        storage.saveTransactions(listOf(transaction))

        val transactionWithInternal = TransactionWithInternal(transaction)

        if (!isEmpty(transactionWithInternal)) {
            listener?.onUpdateTransactions(listOf(transactionWithInternal))
        }
    }

}*/
